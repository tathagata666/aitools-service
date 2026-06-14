package com.costroom.aitoolsservice.provider;

import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.AiUsageSnapshot;

import java.util.List;

/**
 * Contract every AI provider client must implement.
 *
 * fetchSnapshots() is called by the scheduler for every active tool.
 * It should return all raw snapshot rows to be saved — the scheduler
 * handles idempotency (skipping already-saved rows).
 *
 * Adding a new provider later = implement this interface + register in IngestionScheduler.
 */
public interface ProviderClient {

    /** The slug this client handles, e.g. "openai". Must match SupportedProvider.getSlug(). */
    String getProviderSlug();

    /**
     * Fetch usage/cost/seat data from the provider API using the given tool's (decrypted) key.
     *
     * @param tool        the AiTool entity (use to get orgId, keyType, etc.)
     * @param decryptedKey the plaintext API key for the provider
     * @param startEpoch  Unix epoch seconds — look back from this point (typically 30 days ago)
     * @return list of snapshot rows ready to persist (id/ingestedAt not set — JPA handles those)
     */
    List<AiUsageSnapshot> fetchSnapshots(AiTool tool, String decryptedKey, long startEpoch);
}
