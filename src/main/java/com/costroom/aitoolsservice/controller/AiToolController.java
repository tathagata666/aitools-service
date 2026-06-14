package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.dto.AiToolResponse;
import com.costroom.aitoolsservice.dto.CreateAiToolRequest;
import com.costroom.aitoolsservice.dto.UpdateAiToolRequest;
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
 * user_id and org_id are resolved from the JWT — never supplied by the client.
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

    public AiToolController(AiToolService toolService, JwtHelper jwtHelper) {
        this.toolService = toolService;
        this.jwtHelper = jwtHelper;
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

    private UUID resolveUserId(Jwt jwt) {
        return UUID.fromString(jwtHelper.getSubject(jwt));
    }

    /**
     * Resolves orgId from the "custom:org_id" Cognito claim (set by org-service at onboarding).
     * Falls back to zero-UUID so the row is still created; in practice every customer JWT
     * will carry this claim.
     */
    private UUID resolveOrgId(Jwt jwt) {
        String orgIdClaim = jwt.getClaimAsString("custom:org_id");
        if (orgIdClaim != null && !orgIdClaim.isBlank()) {
            return UUID.fromString(orgIdClaim);
        }
        return new UUID(0, 0);
    }
}
