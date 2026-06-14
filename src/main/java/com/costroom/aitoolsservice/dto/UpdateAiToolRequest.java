package com.costroom.aitoolsservice.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update request for an AI tool.
 * Only non-null fields are applied.
 * apiKey — if provided, re-encrypts and replaces the stored key.
 */
public record UpdateAiToolRequest(

    @Size(max = 255, message = "displayName must be 255 characters or fewer")
    String displayName,

    @Size(max = 1024, message = "apiKey must be 1024 characters or fewer")
    String apiKey
) {}
