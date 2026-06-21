package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import com.costroom.aitoolsservice.exception.OrgResolutionException;
import com.costroom.aitoolsservice.repository.AiUsageSnapshotRepository;
import com.costroom.aitoolsservice.repository.UserIdentityRepository;
import com.costroom.aitoolsservice.repository.UserOrgLinkRepository;
import com.costroom.aitoolsservice.security.JwtHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read-only API for querying stored usage snapshots.
 *
 * Routes:
 *   GET /api/snapshots                           – all snapshots for my org
 *   GET /api/snapshots?provider=openai            – filter by provider
 *   GET /api/snapshots?provider=openai&from=&to=  – time-windowed filter
 */
@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    private final AiUsageSnapshotRepository snapshotRepository;
    private final JwtHelper jwtHelper;
    private final UserIdentityRepository userIdentityRepository;
    private final UserOrgLinkRepository userOrgLinkRepository;

    public SnapshotController(AiUsageSnapshotRepository snapshotRepository,
                               JwtHelper jwtHelper,
                               UserIdentityRepository userIdentityRepository,
                               UserOrgLinkRepository userOrgLinkRepository) {
        this.snapshotRepository = snapshotRepository;
        this.jwtHelper = jwtHelper;
        this.userIdentityRepository = userIdentityRepository;
        this.userOrgLinkRepository = userOrgLinkRepository;
    }

    @GetMapping
    public ResponseEntity<List<AiUsageSnapshot>> getSnapshots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        UUID orgId = resolveOrgId(jwt);

        List<AiUsageSnapshot> results;

        if (provider != null && from != null && to != null) {
            results = snapshotRepository.findByOrgIdAndProviderAndBucketStartTimeBetween(
                    orgId, provider.toLowerCase(), from, to);
        } else if (provider != null) {
            long endEpoch   = java.time.Instant.now().getEpochSecond();
            long startEpoch = endEpoch - (30L * 86400);
            results = snapshotRepository.findByOrgIdAndProviderAndBucketStartTimeBetween(
                    orgId, provider.toLowerCase(), startEpoch, endEpoch);
        } else {
            results = snapshotRepository.findAllByOrgIdOrdered(orgId);
        }

        return ResponseEntity.ok(results);
    }

    private UUID resolveOrgId(Jwt jwt) {
        String cognitoSub = jwtHelper.getSubject(jwt);

        UUID internalUserId = userIdentityRepository.findUserIdByCognitoSub(cognitoSub)
                .orElseThrow(() -> new OrgResolutionException(
                        "No internal user found for this identity."));

        return userOrgLinkRepository.findByUserId(internalUserId)
                .map(link -> link.getOrgId())
                .orElseThrow(() -> new OrgResolutionException(
                        "This user is not linked to any organization yet."));
    }
}