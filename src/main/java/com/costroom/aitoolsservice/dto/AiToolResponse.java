package com.costroom.aitoolsservice.dto;

import com.costroom.aitoolsservice.entity.AiTool;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe response for an AI tool — never exposes the encrypted key.
 * A masked key hint (last 4 chars) helps the user identify which key is stored.
 */
public record AiToolResponse(
    UUID    id,
    UUID    userId,
    UUID    orgId,
    String  aiName,
    String  displayName,
    String  keyType,
    String  maskedKey,   // e.g. "••••••••••••abcd"
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static AiToolResponse from(AiTool tool) {
        return new AiToolResponse(
            tool.getId(),
            tool.getUserId(),
            tool.getOrgId(),
            tool.getAiName(),
            tool.getDisplayName(),
            tool.getKeyType(),
            maskKey(tool.getEncryptedKey()),
            tool.isActive(),
            tool.getCreatedAt(),
            tool.getUpdatedAt()
        );
    }

    /**
     * Returns a masked version of the encrypted blob.
     * Since the stored value is base64(IV + ciphertext), we just show
     * "••••[last 4 of plaintext]" — but we don't have plaintext here,
     * so we just show a fixed mask. The UI can show this for visual confirmation
     * that a key is stored without revealing anything.
     */
    private static String maskKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.length() < 8) return "••••••••";
        return "••••••••••••" + encryptedKey.substring(encryptedKey.length() - 4);
    }
}
