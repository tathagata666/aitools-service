package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.dto.AiToolResponse;
import com.costroom.aitoolsservice.dto.CreateAiToolRequest;
import com.costroom.aitoolsservice.dto.UpdateAiToolRequest;
import com.costroom.aitoolsservice.exception.OrgResolutionException;
import com.costroom.aitoolsservice.repository.UserIdentityRepository;
import com.costroom.aitoolsservice.repository.UserOrgLinkRepository;
import com.costroom.aitoolsservice.security.JwtHelper;
import com.costroom.aitoolsservice.service.AiToolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD endpoints for customer AI tool entries.
 *
 * All routes require a valid Cognito JWT with a CUSTOMER_* role.
 * user_id stored on ai_tools is the Cognito sub, straight from the JWT.
 * org_id is resolved server-side via user_identities -> user_org_link,
 * never trusted from a JWT claim or the request body.
 *
 * Routes:
 *   POST   /api/ai-tools           – register a new AI tool
 *   GET    /api/ai-tools           – list my tools
 *   GET    /api/ai-tools/{id}      – get one tool
 *   PATCH  /api/ai-tools/{id}      – update displayName or rotate apiKey
 *   DELETE /api/ai-tools/{id}      – soft-delete tool
 */
@RestController
@RequestMapping("/api/ai-tools")
public class AiToolController {

    private final AiToolService toolService;
    private final JwtHelper jwtHelper;
    private final UserIdentityRepository userIdentityRepository;
    private final UserOrgLinkRepository userOrgLinkRepository;

    public AiToolController(AiToolService toolService,
                             JwtHelper jwtHelper,
                             UserIdentityRepository userIdentityRepository,
                             UserOrgLinkRepository userOrgLinkRepository) {
        this.toolService = toolService;
        this.jwtHelper = jwtHelper;
        this.userIdentityRepository = userIdentityRepository;
        this.userOrgLinkRepository = userOrgLinkRepository;
    }

    @PostMapping
    public ResponseEntity<AiToolResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAiToolRequest req) {

        UUID userId = resolveUserId(jwt);
        UUID orgId  = resolveOrgId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toolService.create(userId, orgId, req));
    }

    @GetMapping
    public ResponseEntity<List<AiToolResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(toolService.listForUser(resolveUserId(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiToolResponse> getOne(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        return ResponseEntity.ok(toolService.getForUser(id, resolveUserId(jwt)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AiToolResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAiToolRequest req) {

        return ResponseEntity.ok(toolService.update(id, resolveUserId(jwt), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        toolService.delete(id, resolveUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** user_id stored on ai_tools rows is the Cognito sub, straight from the JWT. */
    private UUID resolveUserId(Jwt jwt) {
        return UUID.fromString(jwtHelper.getSubject(jwt));
    }

    /**
     * Resolves orgId server-side:
     *   JWT sub -> user_identities.provider_subject -> user_identities.user_id
     *           -> user_org_link.user_id -> user_org_link.org_id
     *
     * No Cognito custom claim involved. Throws OrgResolutionException
     * (mapped to 409 by GlobalExceptionHandler) if either hop is missing,
     * instead of silently falling back to a zero-UUID.
     */
    private UUID resolveOrgId(Jwt jwt) {
        String cognitoSub = jwtHelper.getSubject(jwt);

        UUID internalUserId = userIdentityRepository.findUserIdByCognitoSub(cognitoSub)
                .orElseThrow(() -> new OrgResolutionException(
                        "No internal user found for this identity. " +
                        "User may not be fully provisioned in auth-service yet."));

        return userOrgLinkRepository.findByUserId(internalUserId)
                .map(link -> link.getOrgId())
                .orElseThrow(() -> new OrgResolutionException(
                        "This user is not linked to any organization yet."));
    }
}