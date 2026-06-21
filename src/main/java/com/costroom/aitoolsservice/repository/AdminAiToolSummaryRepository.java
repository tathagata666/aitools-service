package com.costroom.aitoolsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdminAiToolSummaryRepository extends JpaRepository<com.costroom.aitoolsservice.entity.AiTool, UUID> {

    /**
     * at.user_id is the Cognito sub (not users.id), so we resolve email via
     * user_identities first, then join organizations directly on org_id.
     */
    @Query(value = """
            SELECT
                HEX(at.id)      AS toolId,
                HEX(at.user_id) AS userId,
                u.email         AS email,
                HEX(at.org_id)  AS orgId,
                o.name          AS orgName,
                at.ai_name      AS aiName,
                at.display_name AS displayName,
                at.is_active    AS active
            FROM ai_tools at
            LEFT JOIN user_identities ui
                   ON ui.provider_subject = LOWER(HEX(at.user_id))
                   OR ui.provider_subject = CONCAT(
                        LOWER(HEX(SUBSTRING(at.user_id,1,4))), '-',
                        LOWER(HEX(SUBSTRING(at.user_id,5,2))), '-',
                        LOWER(HEX(SUBSTRING(at.user_id,7,2))), '-',
                        LOWER(HEX(SUBSTRING(at.user_id,9,2))), '-',
                        LOWER(HEX(SUBSTRING(at.user_id,11,6)))
                   )
            LEFT JOIN users u
                   ON u.id = ui.user_id
            LEFT JOIN organizations o
                   ON o.id = at.org_id
            ORDER BY at.created_at DESC
            """, nativeQuery = true)
    List<AdminAiToolSummaryRow> findAllSummaries();

    interface AdminAiToolSummaryRow {
        String getToolId();
        String getUserId();
        String getEmail();
        String getOrgId();
        String getOrgName();
        String getAiName();
        String getDisplayName();
        Boolean getActive();
    }
}