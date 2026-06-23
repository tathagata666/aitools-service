package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.dto.AdminUserSummary;
import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.Organization;
import com.costroom.aitoolsservice.entity.User;
import com.costroom.aitoolsservice.entity.UserOrgLink;
import com.costroom.aitoolsservice.repository.AiToolRepository;
import com.costroom.aitoolsservice.repository.OrganizationRepository;
import com.costroom.aitoolsservice.repository.UserIdentityRepository;
import com.costroom.aitoolsservice.repository.UserOrgLinkRepository;
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
 * Platform-admin-only directory: every user, their org, and their AI tools.
 *
 * Restricted to the PLATFORM_ADMIN Cognito group.
 * Never returns API keys, masked or otherwise.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserOrgLinkRepository userOrgLinkRepository;
    private final OrganizationRepository organizationRepository;
    private final AiToolRepository aiToolRepository;
    private final UserIdentityRepository userIdentityRepository;

    public AdminUserController(UserRepository userRepository,
                                UserOrgLinkRepository userOrgLinkRepository,
                                OrganizationRepository organizationRepository,
                                AiToolRepository aiToolRepository,
                                UserIdentityRepository userIdentityRepository) {
        this.userRepository = userRepository;
        this.userOrgLinkRepository = userOrgLinkRepository;
        this.organizationRepository = organizationRepository;
        this.aiToolRepository = aiToolRepository;
        this.userIdentityRepository = userIdentityRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<AdminUserSummary>> listAll() {
        List<User> users = userRepository.findAll();

        Map<UUID, UUID> userIdToOrgId = userOrgLinkRepository.findAll().stream()
                .collect(Collectors.toMap(UserOrgLink::getUserId, UserOrgLink::getOrgId, (a, b) -> a));

        Map<UUID, String> orgIdToName = organizationRepository.findAll().stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName, (a, b) -> a));

        // ai_tools.user_id is the Cognito sub, so build sub -> internal userId,
        // then invert to internal userId -> list of tools
        Map<String, UUID> subToUserId = userIdentityRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ui -> ui.getProviderSubject(),
                        ui -> ui.getUserId(),
                        (a, b) -> a));

        Map<UUID, List<AiTool>> userIdToTools = aiToolRepository.findAll().stream()
                .filter(tool -> subToUserId.containsKey(tool.getUserId().toString()))
                .collect(Collectors.groupingBy(tool -> subToUserId.get(tool.getUserId().toString())));

        List<AdminUserSummary> result = users.stream()
                .map(u -> {
                    UUID orgId = userIdToOrgId.get(u.getId());
                    String orgName = orgId != null ? orgIdToName.get(orgId) : null;

                    List<AdminUserSummary.ToolSummary> tools = userIdToTools
                            .getOrDefault(u.getId(), List.of())
                            .stream()
                            .map(t -> new AdminUserSummary.ToolSummary(
                                    t.getId(), t.getAiName(), t.getDisplayName(), t.isActive()))
                            .toList();

                    return new AdminUserSummary(
                            u.getId(), u.getEmail(), u.getRole(), u.getStatus(),
                            u.getInvitedAt(), u.getAcceptedAt(), u.getCreatedAt(),
                            orgId, orgName, tools
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}