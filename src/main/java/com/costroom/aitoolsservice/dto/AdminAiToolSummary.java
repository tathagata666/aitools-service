package com.costroom.aitoolsservice.dto;

import java.util.UUID;

/**
 * Platform-admin-only projection: who owns which AI tool, in which org.
 * Deliberately excludes the encrypted/decrypted key entirely.
 */
public record AdminAiToolSummary(
        UUID toolId,
        UUID userId,
        String email,
        UUID orgId,
        String orgName,
        String aiName,
        String displayName,
        boolean active
) {
}