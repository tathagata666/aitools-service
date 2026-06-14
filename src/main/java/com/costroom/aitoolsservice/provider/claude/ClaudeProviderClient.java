package com.costroom.aitoolsservice.provider.claude;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches usage data from the Anthropic Admin API.
 *
 * Requires an Admin API key with usage:read permission.
 * API reference: https://docs.anthropic.com/en/api/usage
 */
@Component
public class ClaudeProviderClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProviderClient.class);
    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;

    public ClaudeProviderClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String getProviderSlug() {
        return "claude";
    }

    @Override
    public List<AiUsageSnapshot> fetchSnapshots(AiTool tool, String decryptedKey, long startEpoch) {
        List<AiUsageSnapshot> snapshots = new ArrayList<>();
        try {
            String startDate = Instant.ofEpochSecond(startEpoch)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString(); // yyyy-MM-dd

            ClaudeUsageResponse response = restClient.get()
                    .uri("/v1/usage?start_date={d}&granularity=day", startDate)
                    .header("x-api-key", decryptedKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->
                        log.warn("Claude usage API error for org {}: status={}", tool.getOrgId(), res.getStatusCode())
                    )
                    .body(ClaudeUsageResponse.class);

            if (response == null || response.data() == null) return snapshots;

            for (UsageEntry entry : response.data()) {
                long bucketEpoch = LocalDate.parse(entry.date())
                        .atStartOfDay(ZoneOffset.UTC)
                        .toEpochSecond();

                snapshots.add(AiUsageSnapshot.builder()
                        .orgId(tool.getOrgId())
                        .aiToolId(tool.getId())
                        .provider("claude")
                        .modelId(entry.model())
                        .snapshotType("completions")
                        .sourceType("usage")
                        .bucketStartTime(bucketEpoch)
                        .inputTokens(entry.inputTokens())
                        .outputTokens(entry.outputTokens())
                        .inputCachedTokens(entry.cacheReadInputTokens())
                        .totalRequests(entry.requestCount())
                        .costUsd(entry.costUsd())
                        .build());
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Claude usage for org {}: {}", tool.getOrgId(), e.getMessage());
        }
        return snapshots;
    }

    // ── Response DTOs (package-private so they're accessible as plain types) ──

    record UsageEntry(
        String date,
        String model,
        @JsonProperty("workspace_id")                String workspaceId,
        @JsonProperty("input_tokens")                Long   inputTokens,
        @JsonProperty("output_tokens")               Long   outputTokens,
        @JsonProperty("cache_read_input_tokens")     Long   cacheReadInputTokens,
        @JsonProperty("cache_creation_input_tokens") Long   cacheCreationInputTokens,
        @JsonProperty("request_count")               Long   requestCount,
        @JsonProperty("cost_usd")                    BigDecimal costUsd
    ) {}

    record ClaudeUsageResponse(List<UsageEntry> data) {}
}
