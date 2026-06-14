package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.AiTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiToolRepository extends JpaRepository<AiTool, UUID> {

    /** All active tools owned by a specific user. */
    List<AiTool> findByUserIdAndActiveTrue(UUID userId);

    /** All active tools for an org (used by scheduler). */
    List<AiTool> findByOrgIdAndActiveTrue(UUID orgId);

    /** Get a single tool by id + owner check. */
    Optional<AiTool> findByIdAndUserId(UUID id, UUID userId);

    /** Distinct org IDs that have at least one active tool (for scheduler). */
    @Query("SELECT DISTINCT t.orgId FROM AiTool t WHERE t.active = true")
    List<UUID> findDistinctActiveOrgIds();

    /** All active tools for an org filtered by provider (for scheduler per-provider runs). */
    List<AiTool> findByOrgIdAndAiNameAndActiveTrue(UUID orgId, String aiName);

    /** Duplicate check: does this org already have an active key for this provider+keyType? */
    boolean existsByOrgIdAndAiNameAndKeyTypeAndActiveTrue(UUID orgId, String aiName, String keyType);
}
