package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.entity.AiUsageSnapshot;
import com.costroom.aitoolsservice.repository.AiUsageSnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read-only API for querying stored usage snapshots.
 *
 * Used by:
 *   - The React frontend (via customer JWT) to show basic usage history
 *   - The aianalytics-service (also via customer JWT forwarding) to pull raw data
 *
 * Routes:
 *   GET /api/snapshots                         – all snapshots for my org
 *   GET /api/snapshots?provider=openai         – filter by provider
 *   GET /api/snapshots?provider=openai&from=&to= – time-windowed filter
 */
@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    private final AiUsageSnapshotRepository snapshotRepository;

    public SnapshotController(AiUsageSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping
    public ResponseEntity<List<AiUsageSnapshot>> getSnapshots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Long from,    // Unix epoch seconds
            @RequestParam(required = false) Long to) {    // Unix epoch seconds

        UUID orgId = resolveOrgId(jwt);

        List<AiUsageSnapshot> results;

        if (provider != null && from != null && to != null) {
            results = snapshotRepository.findByOrgIdAndProviderAndBucketStartTimeBetween(
                    orgId, provider.toLowerCase(), from, to);
        } else if (provider != null) {
            // Default window: last 30 days
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
        String orgIdClaim = jwt.getClaimAsString("custom:org_id");
        if (orgIdClaim != null && !orgIdClaim.isBlank()) {
            return UUID.fromString(orgIdClaim);
        }
        return new UUID(0, 0);
    }
}
