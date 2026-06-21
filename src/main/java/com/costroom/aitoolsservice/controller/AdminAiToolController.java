package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.dto.AdminAiToolSummary;
import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.Organization;
import com.costroom.aitoolsservice.entity.User;
import com.costroom.aitoolsservice.entity.UserIdentity;
import com.costroom.aitoolsservice.repository.AiToolRepository;
import com.costroom.aitoolsservice.repository.OrganizationRepository;
import com.costroom.aitoolsservice.repository.UserIdentityRepository;
import com.costroom.aitoolsservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Platform-admin-only visibility into who owns which AI tool.
 *
 * Returns metadata only — toolId, userId, email, orgId, orgName, aiName,
 * displayName, active. Never the API key, masked or otherwise; use the
 * existing per-user GET /api/ai-tools for masked-key visibility.
 *
 * Restricted to the PLATFORM_ADMIN Cognito group (same group used to call
 * POST /api/admin/users in auth-service).
 */
@RestController
@RequestMapping("/api/admin/ai-tools")
public class AdminAiToolController {

    private final AiToolRepository aiToolRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public AdminAiToolController(AiToolRepository aiToolRepository,
                                  UserIdentityRepository userIdentityRepository,
                                  UserRepository userRepository,
                                  OrganizationRepository organizationRepository) {
        this.aiToolRepository = aiToolRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<AdminAiToolSummary>> listAll() {
        List<AiTool> tools = aiToolRepository.findAll();

        // Build sub -> internal userId lookup once, not per-row
        Map<String, UUID> subToUserId = userIdentityRepository.findAll().stream()
                .collect(Collectors.toMap(
                        UserIdentity::getProviderSubject,
                        UserIdentity::getUserId,
                        (a, b) -> a));

        Map<UUID, String> userIdToEmail = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));

        Map<UUID, String> orgIdToName = organizationRepository.findAll().stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName, (a, b) -> a));

        List<AdminAiToolSummary> result = tools.stream()
                .map(tool -> {
                    String cognitoSub = tool.getUserId().toString();
                    UUID internalUserId = subToUserId.get(cognitoSub);
                    String email = internalUserId != null
                            ? userIdToEmail.get(internalUserId)
                            : null;
                    String orgName = orgIdToName.get(tool.getOrgId());

                    return new AdminAiToolSummary(
                            tool.getId(),
                            tool.getUserId(),
                            email,
                            tool.getOrgId(),
                            orgName,
                            tool.getAiName(),
                            tool.getDisplayName(),
                            tool.isActive()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}