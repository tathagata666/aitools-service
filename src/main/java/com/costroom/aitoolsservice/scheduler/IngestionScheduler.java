package com.costroom.aitoolsservice.scheduler;

import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import com.costroom.aitoolsservice.provider.ProviderClient;
import com.costroom.aitoolsservice.repository.AiToolRepository;
import com.costroom.aitoolsservice.repository.AiUsageSnapshotRepository;
import com.costroom.aitoolsservice.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs every 6 hours (configurable via INGESTION_RATE_MS env var).
 *
 * For each active org:
 *   1. Load all active AI tools for the org.
 *   2. For each tool, find the matching ProviderClient by aiName slug.
 *   3. Call fetchSnapshots() — returns raw rows for the last 30 days.
 *   4. De-duplicate via existsBy... check and save only new rows.
 *
 * Per-org and per-tool errors are isolated — one bad key won't stop other orgs.
 *
 * Adding a new provider: implement ProviderClient + annotate @Component.
 * Spring auto-wires all ProviderClient beans into the list below.
 */
@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);
    private static final int LOOKBACK_DAYS = 30;

    private final AiToolRepository toolRepository;
    private final AiUsageSnapshotRepository snapshotRepository;
    private final EncryptionService encryptionService;
    private final Map<String, ProviderClient> clientsBySlug;

    public IngestionScheduler(
            AiToolRepository toolRepository,
            AiUsageSnapshotRepository snapshotRepository,
            EncryptionService encryptionService,
            List<ProviderClient> providerClients) {

        this.toolRepository = toolRepository;
        this.snapshotRepository = snapshotRepository;
        this.encryptionService = encryptionService;
        // Index clients by their slug for O(1) lookup
        this.clientsBySlug = providerClients.stream()
                .collect(Collectors.toMap(ProviderClient::getProviderSlug, Function.identity()));

        log.info("IngestionScheduler initialised with {} provider client(s): {}",
                clientsBySlug.size(), clientsBySlug.keySet());
    }

    @Scheduled(fixedRateString = "${app.ingestion.fixed-rate-ms:21600000}")
    public void runIngestion() {
        long startEpoch = Instant.now().minus(Duration.ofDays(LOOKBACK_DAYS)).getEpochSecond();

        List<UUID> orgIds = toolRepository.findDistinctActiveOrgIds();
        if (orgIds.isEmpty()) {
            log.debug("No orgs with active AI tools — skipping ingestion run.");
            return;
        }

        log.info("Starting ingestion run for {} org(s)", orgIds.size());

        for (UUID orgId : orgIds) {
            try {
                ingestForOrg(orgId, startEpoch);
            } catch (Exception e) {
                log.warn("Ingestion failed for org {}: {}", orgId, e.getMessage());
            }
        }

        log.info("Ingestion run complete.");
    }

    // ── Per-org ───────────────────────────────────────────────────────────────

    private void ingestForOrg(UUID orgId, long startEpoch) {
        List<AiTool> tools = toolRepository.findByOrgIdAndActiveTrue(orgId);

        for (AiTool tool : tools) {
            ProviderClient client = clientsBySlug.get(tool.getAiName());
            if (client == null) {
                log.warn("No ProviderClient registered for provider '{}' (tool id={})",
                        tool.getAiName(), tool.getId());
                continue;
            }

            try {
                String decryptedKey = encryptionService.decrypt(tool.getEncryptedKey());
                List<AiUsageSnapshot> snapshots = client.fetchSnapshots(tool, decryptedKey, startEpoch);
                int saved = saveDeduped(snapshots);
                log.info("Org={} provider={} tool={}: fetched={} saved={}",
                        orgId, tool.getAiName(), tool.getId(), snapshots.size(), saved);
            } catch (Exception e) {
                log.warn("Ingestion failed for tool {} (org={} provider={}): {}",
                        tool.getId(), orgId, tool.getAiName(), e.getMessage());
            }
        }
    }

    // ── Deduplication ─────────────────────────────────────────────────────────

    private int saveDeduped(List<AiUsageSnapshot> snapshots) {
        int saved = 0;
        for (AiUsageSnapshot s : snapshots) {
            boolean exists = snapshotRepository
                    .existsByOrgIdAndAiToolIdAndProviderAndSnapshotTypeAndModelIdAndBucketStartTimeAndSourceType(
                            s.getOrgId(),
                            s.getAiToolId(),
                            s.getProvider(),
                            s.getSnapshotType(),
                            s.getModelId(),
                            s.getBucketStartTime(),
                            s.getSourceType()
                    );
            if (!exists) {
                snapshotRepository.save(s);
                saved++;
            }
        }
        return saved;
    }
}
