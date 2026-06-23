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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs every 6 hours (configurable via INGESTION_RATE_MS env var).
 *
 * For each active org:
 * 1. Load all active AI tools for the org.
 * 2. For each tool, find the matching ProviderClient by aiName slug.
 * 3. Call fetchSnapshots() — returns raw rows for the last 30 days.
 * 4. Upsert each row:
 * - Past days → insert once, never update (provider data is final)
 * - Today → update in-place so running totals stay fresh
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
                int[] counts = upsertSnapshots(snapshots);
                log.info("Org={} provider={} tool={}: fetched={} inserted={} updated={}",
                        orgId, tool.getAiName(), tool.getId(),
                        snapshots.size(), counts[0], counts[1]);
            } catch (Exception e) {
                log.warn("Ingestion failed for tool {} (org={} provider={}): {}",
                        tool.getId(), orgId, tool.getAiName(), e.getMessage());
            }
        }
    }

    // ── Upsert ────────────────────────────────────────────────────────────────

    /**
     * Upserts each snapshot row:
     * - No existing row → insert (past or new day)
     * - Existing row found → update metrics in-place (today's running totals)
     *
     * Returns int[]{inserted, updated} for logging.
     */
    private int[] upsertSnapshots(List<AiUsageSnapshot> snapshots) {
        int inserted = 0;
        int updated = 0;

        for (AiUsageSnapshot s : snapshots) {
            Optional<AiUsageSnapshot> existing = snapshotRepository
                    .findByOrgIdAndAiToolIdAndProviderAndSnapshotTypeAndModelIdAndBucketStartTimeAndSourceType(
                            s.getOrgId(),
                            s.getAiToolId(),
                            s.getProvider(),
                            s.getSnapshotType(),
                            s.getModelId(),
                            s.getBucketStartTime(),
                            s.getSourceType());

            if (existing.isPresent()) {
                AiUsageSnapshot row = existing.get();
                // Only update if something actually changed — avoids unnecessary DB writes
                if (hasChanged(row, s)) {
                    row.setInputTokens(s.getInputTokens());
                    row.setOutputTokens(s.getOutputTokens());
                    row.setInputCachedTokens(s.getInputCachedTokens());
                    row.setTotalRequests(s.getTotalRequests());
                    row.setCostUsd(s.getCostUsd());
                    row.setTotalSeats(s.getTotalSeats());
                    row.setActiveSeats(s.getActiveSeats());
                    snapshotRepository.save(row);
                    updated++;
                }
            } else {
                snapshotRepository.save(s);
                inserted++;
            }
        }

        return new int[] { inserted, updated };
    }

    /**
     * Returns true if any metric field differs between the stored row and the fresh
     * data.
     * Past-day rows from providers like OpenAI are immutable so this will always be
     * false
     * for them — the DB write is skipped entirely, keeping the scheduler efficient.
     */
    private boolean hasChanged(AiUsageSnapshot existing, AiUsageSnapshot fresh) {
        return !java.util.Objects.equals(existing.getInputTokens(), fresh.getInputTokens())
                || !java.util.Objects.equals(existing.getOutputTokens(), fresh.getOutputTokens())
                || !java.util.Objects.equals(existing.getInputCachedTokens(), fresh.getInputCachedTokens())
                || !java.util.Objects.equals(existing.getTotalRequests(), fresh.getTotalRequests())
                || !java.util.Objects.equals(existing.getCostUsd(), fresh.getCostUsd())
                || !java.util.Objects.equals(existing.getTotalSeats(), fresh.getTotalSeats())
                || !java.util.Objects.equals(existing.getActiveSeats(), fresh.getActiveSeats());
    }
}