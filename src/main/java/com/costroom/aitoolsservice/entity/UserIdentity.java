package com.costroom.aitoolsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Read-only mapping of Cognito sub -> internal users.id.
 * Owned by auth-service; aitools-service only reads it to resolve the
 * caller's internal user id from their JWT subject.
 */
@Entity
@Table(name = "user_identities")
public class UserIdentity {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "provider_subject", nullable = false, length = 128)
    private String providerSubject;

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProviderSubject() {
        return providerSubject;
    }
}