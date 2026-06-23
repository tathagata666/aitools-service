package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageSnapshotRepository extends JpaRepository<AiUsageSnapshot, UUID> {

    /**
     * Lookup for upsert — returns the existing row for a given time bucket if one exists.
     * Used by IngestionScheduler to decide whether to insert or update.
     */
    Optional<AiUsageSnapshot> findByOrgIdAndAiToolIdAndProviderAndSnapshotTypeAndModelIdAndBucketStartTimeAndSourceType(
            UUID orgId,
            UUID aiToolId,
            String provider,
            String snapshotType,
            String modelId,
            Long bucketStartTime,
            String sourceType
    );

    /** For the analytics service to query — all snapshots for an org/provider window. */
    List<AiUsageSnapshot> findByOrgIdAndProviderAndBucketStartTimeBetween(
            UUID orgId,
            String provider,
            Long startEpoch,
            Long endEpoch
    );

    /** All snapshots for an org, across all providers, ordered by time. */
    @Query("SELECT s FROM AiUsageSnapshot s WHERE s.orgId = :orgId ORDER BY s.bucketStartTime ASC")
    List<AiUsageSnapshot> findAllByOrgIdOrdered(@Param("orgId") UUID orgId);
}