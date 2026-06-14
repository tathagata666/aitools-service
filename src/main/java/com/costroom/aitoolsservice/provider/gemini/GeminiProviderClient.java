package com.costroom.aitoolsservice.provider.gemini;

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
 * Fetches usage data from the Google AI Studio usage API for Gemini models.
 *
 * The API key is a Google AI Studio key (generativelanguage.googleapis.com access).
 * API reference: https://ai.google.dev/api
 */
@Component
public class GeminiProviderClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiProviderClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private final RestClient restClient;

    public GeminiProviderClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String getProviderSlug() {
        return "gemini";
    }

    @Override
    public List<AiUsageSnapshot> fetchSnapshots(AiTool tool, String decryptedKey, long startEpoch) {
        List<AiUsageSnapshot> snapshots = new ArrayList<>();
        try {
            GeminiUsageResponse response = restClient.get()
                    .uri("/v1beta/usage?key={key}&pageSize=100", decryptedKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->
                        log.warn("Gemini usage API error for org {}: status={}", tool.getOrgId(), res.getStatusCode())
                    )
                    .body(GeminiUsageResponse.class);

            if (response == null || response.usageRecords() == null) return snapshots;

            for (UsageRecord record : response.usageRecords()) {
                long bucketEpoch = startEpoch;
                if (record.timestamp() != null) {
                    try {
                        bucketEpoch = Instant.parse(record.timestamp()).getEpochSecond();
                    } catch (Exception ignored) {}
                }

                snapshots.add(AiUsageSnapshot.builder()
                        .orgId(tool.getOrgId())
                        .aiToolId(tool.getId())
                        .provider("gemini")
                        .modelId(record.model())
                        .snapshotType("completions")
                        .sourceType("usage")
                        .bucketStartTime(bucketEpoch)
                        .inputTokens(record.promptTokenCount())
                        .outputTokens(record.candidatesTokenCount())
                        .totalRequests(record.requestCount())
                        .build());
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Gemini usage for org {}: {}", tool.getOrgId(), e.getMessage());
        }
        return snapshots;
    }

    // ── Response DTOs (package-private, declared before the wrapper) ──────────

    record UsageRecord(
        String model,
        String timestamp,
        @JsonProperty("promptTokenCount")      Long promptTokenCount,
        @JsonProperty("candidatesTokenCount")  Long candidatesTokenCount,
        @JsonProperty("totalTokenCount")       Long totalTokenCount,
        @JsonProperty("requestCount")          Long requestCount
    ) {}

    record GeminiUsageResponse(
        @JsonProperty("usageRecords")  List<UsageRecord> usageRecords,
        @JsonProperty("nextPageToken") String nextPageToken
    ) {}
}
