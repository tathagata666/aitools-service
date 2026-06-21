package com.costroom.aitoolsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only mapping of users.id -> organizations.id.
 * Owned by org-service; aitools-service only reads it to resolve which
 * organization the caller belongs to.
 */
@Entity
@Table(name = "user_org_link")
public class UserOrgLink {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "org_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID orgId;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}