package com.costroom.aitoolsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Raw usage/cost snapshot fetched from an AI provider API.
 *
 * Designed to be provider-agnostic:
 *   - Token-based providers (OpenAI, Claude, Gemini) → populate token/request columns
 *   - Seat-based providers (GitHub Copilot)          → populate seat columns
 *   - Cost data                                      → populate cost_usd
 *
 * The analytics service (aianalytics-service) reads this table to produce
 * aggregations, forecasts, and burn-rate alerts.
 */
@Entity
@Table(
    name = "ai_usage_snapshots",
    indexes = {
        @Index(name = "idx_snapshots_org_bucket",   columnList = "org_id, bucket_start_time"),
        @Index(name = "idx_snapshots_tool_id",       columnList = "ai_tool_id"),
        @Index(name = "idx_snapshots_provider",      columnList = "provider, model_id"),
        @Index(name = "idx_snapshots_org_provider",  columnList = "org_id, provider, bucket_start_time")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageSnapshot {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** Organisation that owns these metrics — plain UUID, no local FK. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "org_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orgId;

    /** The ai_tools row that provided the API key for this ingestion. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ai_tool_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID aiToolId;

    /** Provider slug: openai | claude | gemini | github_copilot */
    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    /** Model identifier, e.g. gpt-4o, claude-3-5-sonnet-20241022, gemini-1.5-pro */
    @Column(name = "model_id", length = 255)
    private String modelId;

    /** Metric category: completions | embeddings | cost | seats */
    @Column(name = "snapshot_type", nullable = false, length = 100)
    private String snapshotType;

    /** Which data kind this row represents: usage | cost | seats */
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    /** Unix epoch seconds — start of the provider's time bucket (typically 1 day). */
    @Column(name = "bucket_start_time", nullable = false)
    private Long bucketStartTime;

    // ── Token / request metrics (null for seat-based providers) ──────────────

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "input_cached_tokens")
    private Long inputCachedTokens;

    @Column(name = "total_requests")
    private Long totalRequests;

    // ── Cost ──────────────────────────────────────────────────────────────────

    /** Cost in USD — precision 10 handles micro-cent values from OpenAI billing. */
    @Column(name = "cost_usd", precision = 18, scale = 10)
    private BigDecimal costUsd;

    // ── Seat metrics (GitHub Copilot) ─────────────────────────────────────────

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "active_seats")
    private Integer activeSeats;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        ingestedAt = Instant.now();
    }
}
