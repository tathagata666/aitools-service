package com.costroom.aitoolsservice.provider.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** Response from GET /v1/organization/usage/completions */
record OpenAiUsageResponse(
    String object,
    List<Bucket> data,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("next_page") String nextPage
) {
    record Bucket(
        @JsonProperty("start_time") Long startTime,
        @JsonProperty("end_time")   Long endTime,
        List<Result> results
    ) {}

    record Result(
        @JsonProperty("input_tokens")        Long inputTokens,
        @JsonProperty("output_tokens")       Long outputTokens,
        @JsonProperty("input_cached_tokens") Long inputCachedTokens,
        @JsonProperty("num_model_requests")  Long numModelRequests,
        @JsonProperty("model_ids")           List<String> modelIds
    ) {}
}

/** Response from GET /v1/organization/costs */
record OpenAiCostResponse(
    String object,
    List<Bucket> data,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("next_page") String nextPage
) {
    record Bucket(
        @JsonProperty("start_time") Long startTime,
        @JsonProperty("end_time")   Long endTime,
        List<Result> results
    ) {}

    record Result(
        @JsonProperty("amount")    Amount amount,
        @JsonProperty("line_item") String lineItem,
        @JsonProperty("model_ids") List<String> modelIds
    ) {}

    record Amount(
        BigDecimal value,
        String currency
    ) {}
}
