package com.costroom.aitoolsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for registering a new AI tool.
 *
 * aiName must be one of the supported provider slugs:
 *   openai | claude | gemini | github_copilot
 *
 * keyType is optional; defaults to "default".
 *   OpenAI supports "admin" and "project" key types.
 *
 * displayName is optional. If omitted, the provider display name is used.
 */
public record CreateAiToolRequest(

    @NotBlank(message = "aiName must not be blank")
    @Size(max = 100, message = "aiName must be 100 characters or fewer")
    String aiName,

    @NotBlank(message = "apiKey must not be blank")
    @Size(max = 1024, message = "apiKey must be 1024 characters or fewer")
    String apiKey,

    @Size(max = 255, message = "displayName must be 255 characters or fewer")
    String displayName,

    /** For OpenAI: "admin" or "project". Others: omit or pass "default". */
    @Size(max = 50)
    String keyType
) {}
