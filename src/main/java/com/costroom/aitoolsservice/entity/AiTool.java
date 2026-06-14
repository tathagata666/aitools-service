package com.costroom.aitoolsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single AI tool (provider + encrypted API key) registered by a customer.
 *
 * user_id and org_id are soft-references to the shared costroomdb tables managed
 * by auth-service and org-service — no FK constraints so each service deploys independently.
 *
 * Supported providers: openai | claude | gemini | github_copilot
 * More can be added later without schema changes (provider is a plain VARCHAR).
 */
@Entity
@Table(name = "ai_tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTool {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** Cognito sub from auth-service. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** organizations.id from org-service. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "org_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orgId;

    /**
     * Provider slug — one of: openai, claude, gemini, github_copilot.
     * Stored lowercase. Validated in service layer against the SupportedProvider enum.
     */
    @Column(name = "ai_name", nullable = false, length = 100)
    private String aiName;

    /** Optional human-friendly label, e.g. "Production OpenAI key". */
    @Column(name = "display_name")
    private String displayName;

    /** AES-256-GCM encrypted API key. Never returned in API responses. */
    @Column(name = "encrypted_key", nullable = false, length = 2048)
    private String encryptedKey;

    /**
     * Key type relevant to the provider:
     *   OpenAI  → admin | project
     *   Claude  → default
     *   Gemini  → default
     *   Copilot → default (GitHub PAT with copilot:read scope)
     */
    @Column(name = "key_type", nullable = false, length = 50)
    @Builder.Default
    private String keyType = "default";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
