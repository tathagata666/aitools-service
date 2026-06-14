package com.costroom.aitoolsservice.provider.copilot;

import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import com.costroom.aitoolsservice.provider.ProviderClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches GitHub Copilot seat usage from the GitHub REST API.
 *
 * GitHub Copilot uses seat-based billing — not token-based.
 * This client records total and active seat counts per snapshot run.
 *
 * Required: A GitHub Personal Access Token (PAT) or GitHub App installation
 * token with "manage_billing:copilot" or "read:org" scope.
 *
 * API endpoint:
 *   GET https://api.github.com/orgs/{org}/copilot/billing/seats
 *
 * The GitHub org name must be provided as the displayName of the tool.
 *
 * API reference: https://docs.github.com/en/rest/copilot/copilot-user-management
 */
@Component
public class CopilotProviderClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(CopilotProviderClient.class);
    private static final String BASE_URL = "https://api.github.com";

    private final RestClient restClient;

    public CopilotProviderClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String getProviderSlug() {
        return "github_copilot";
    }

    @Override
    public List<AiUsageSnapshot> fetchSnapshots(AiTool tool, String decryptedKey, long startEpoch) {
        List<AiUsageSnapshot> snapshots = new ArrayList<>();

        // displayName is used as the GitHub org slug (e.g. "my-github-org")
        String githubOrg = tool.getDisplayName();
        if (githubOrg == null || githubOrg.isBlank()) {
            log.warn("Copilot tool {} has no displayName (GitHub org) set — skipping", tool.getId());
            return snapshots;
        }

        try {
            CopilotBillingResponse response = restClient.get()
                    .uri("/orgs/{org}/copilot/billing/seats?per_page=100", githubOrg)
                    .header("Authorization", "Bearer " + decryptedKey)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Copilot API error for org {}: status={}", tool.getOrgId(), res.getStatusCode());
                    })
                    .body(CopilotBillingResponse.class);

            if (response == null) return snapshots;

            // Copilot is seat-based — one snapshot row per ingestion run (current point-in-time)
            long now = Instant.now().getEpochSecond();
            // Round down to midnight UTC for consistent daily bucketing
            long bucketEpoch = (now / 86400) * 86400;

            int activeSeats = 0;
            if (response.seats() != null) {
                activeSeats = (int) response.seats().stream()
                        .filter(s -> s.pendingCancellation() == null || !s.pendingCancellation())
                        .count();
            }

            snapshots.add(AiUsageSnapshot.builder()
                    .orgId(tool.getOrgId())
                    .aiToolId(tool.getId())
                    .provider("github_copilot")
                    .modelId("copilot")
                    .snapshotType("seats")
                    .sourceType("seats")
                    .bucketStartTime(bucketEpoch)
                    .totalSeats(response.totalSeats() != null ? response.totalSeats() : 0)
                    .activeSeats(activeSeats)
                    .build());

        } catch (Exception e) {
            log.warn("Failed to fetch Copilot seats for org {}: {}", tool.getOrgId(), e.getMessage());
        }
        return snapshots;
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    record CopilotBillingResponse(
        @JsonProperty("total_seats") Integer totalSeats,
        @JsonProperty("seats") List<Seat> seats
    ) {}

    record Seat(
        @JsonProperty("created_at")           String createdAt,
        @JsonProperty("updated_at")           String updatedAt,
        @JsonProperty("pending_cancellation") Boolean pendingCancellation,
        @JsonProperty("last_activity_at")     String lastActivityAt,
        @JsonProperty("plan_type")            String planType
    ) {}
}
