package com.costroom.aitoolsservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserSummary(
        UUID userId,
        String email,
        String role,
        String status,
        Instant invitedAt,
        Instant acceptedAt,
        Instant createdAt,
        UUID orgId,
        String orgName,
        List<ToolSummary> tools
) {
    public record ToolSummary(
            UUID toolId,
            String aiName,
            String displayName,
            boolean active
    ) {
    }
}