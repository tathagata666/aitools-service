package com.costroom.aitoolsservice.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum of AI providers currently supported for tracking.
 *
 * Adding a new provider later requires:
 *   1. Adding a new constant here.
 *   2. Implementing a ProviderClient for it.
 *   3. Wiring it in IngestionScheduler.
 *
 * The slug is the value stored in the ai_tools.ai_name column.
 */
public enum SupportedProvider {

    OPENAI("openai", "OpenAI"),
    CLAUDE("claude", "Anthropic Claude"),
    GEMINI("gemini", "Google Gemini"),
    GITHUB_COPILOT("github_copilot", "GitHub Copilot");

    /** Stored in DB and expected in API requests (lowercase, snake_case). */
    private final String slug;

    /** Human-readable display name. */
    private final String displayName;

    SupportedProvider(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Case-insensitive lookup by slug. Returns empty if not supported. */
    public static Optional<SupportedProvider> fromSlug(String slug) {
        if (slug == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(p -> p.slug.equalsIgnoreCase(slug.trim()))
                .findFirst();
    }

    /** Returns all valid slug strings (for error messages). */
    public static String validSlugs() {
        return Arrays.stream(values())
                .map(SupportedProvider::getSlug)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
