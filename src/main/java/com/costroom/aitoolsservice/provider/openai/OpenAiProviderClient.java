package com.costroom.aitoolsservice.provider.openai;

import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import com.costroom.aitoolsservice.provider.ProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches usage (tokens/requests) and cost data from the OpenAI admin API.
 *
 * Requires an Admin API key (sk-admin-...) — Project keys cannot access
 * the organization usage/costs endpoints.
 *
 * OpenAI API docs:
 *   GET /v1/organization/usage/completions  → token counts per model per day
 *   GET /v1/organization/costs              → USD cost per line-item per day
 */
@Component
public class OpenAiProviderClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProviderClient.class);
    private static final String BASE_URL = "https://api.openai.com/v1";

    private final RestClient restClient;

    public OpenAiProviderClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String getProviderSlug() {
        return "openai";
    }

    @Override
    public List<AiUsageSnapshot> fetchSnapshots(AiTool tool, String decryptedKey, long startEpoch) {
        List<AiUsageSnapshot> snapshots = new ArrayList<>();

        // Only admin keys can hit the org-level usage/costs endpoints
        if (!"admin".equalsIgnoreCase(tool.getKeyType())) {
            log.debug("Skipping OpenAI usage fetch for tool {} — key type is '{}', need 'admin'",
                tool.getId(), tool.getKeyType());
            return snapshots;
        }

        snapshots.addAll(fetchUsageSnapshots(tool, decryptedKey, startEpoch));
        snapshots.addAll(fetchCostSnapshots(tool, decryptedKey, startEpoch));
        return snapshots;
    }

    // ── Usage (tokens + request counts) ──────────────────────────────────────

    private List<AiUsageSnapshot> fetchUsageSnapshots(AiTool tool, String key, long startEpoch) {
        List<AiUsageSnapshot> result = new ArrayList<>();
        try {
            OpenAiUsageResponse response = restClient.get()
                    .uri("/organization/usage/completions?start_time={s}&limit=31&bucket_width=1d", startEpoch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("OpenAI usage API error for org {}: status={}", tool.getOrgId(), res.getStatusCode());
                    })
                    .body(OpenAiUsageResponse.class);

            if (response == null || response.data() == null) return result;

            for (OpenAiUsageResponse.Bucket bucket : response.data()) {
                if (bucket.results() == null) continue;
                for (OpenAiUsageResponse.Result r : bucket.results()) {
                    String modelId = (r.modelIds() != null && !r.modelIds().isEmpty())
                            ? r.modelIds().get(0) : "all";

                    result.add(AiUsageSnapshot.builder()
                            .orgId(tool.getOrgId())
                            .aiToolId(tool.getId())
                            .provider("openai")
                            .modelId(modelId)
                            .snapshotType("completions")
                            .sourceType("usage")
                            .bucketStartTime(bucket.startTime())
                            .inputTokens(r.inputTokens())
                            .outputTokens(r.outputTokens())
                            .inputCachedTokens(r.inputCachedTokens())
                            .totalRequests(r.numModelRequests())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch OpenAI usage for org {}: {}", tool.getOrgId(), e.getMessage());
        }
        return result;
    }

    // ── Costs (USD) ───────────────────────────────────────────────────────────

    private List<AiUsageSnapshot> fetchCostSnapshots(AiTool tool, String key, long startEpoch) {
        List<AiUsageSnapshot> result = new ArrayList<>();
        try {
            OpenAiCostResponse response = restClient.get()
                    .uri("/organization/costs?start_time={s}&limit=31&bucket_width=1d", startEpoch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("OpenAI cost API error for org {}: status={}", tool.getOrgId(), res.getStatusCode());
                    })
                    .body(OpenAiCostResponse.class);

            if (response == null || response.data() == null) return result;

            for (OpenAiCostResponse.Bucket bucket : response.data()) {
                if (bucket.results() == null) continue;
                for (OpenAiCostResponse.Result r : bucket.results()) {
                    if (r.amount() == null) continue;

                    String modelId  = (r.modelIds() != null && !r.modelIds().isEmpty())
                            ? r.modelIds().get(0) : "all";
                    String lineItem = r.lineItem() != null ? r.lineItem() : "completions";

                    result.add(AiUsageSnapshot.builder()
                            .orgId(tool.getOrgId())
                            .aiToolId(tool.getId())
                            .provider("openai")
                            .modelId(modelId)
                            .snapshotType(lineItem)
                            .sourceType("cost")
                            .bucketStartTime(bucket.startTime())
                            .costUsd(r.amount().value())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch OpenAI costs for org {}: {}", tool.getOrgId(), e.getMessage());
        }
        return result;
    }
}
