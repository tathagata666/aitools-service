package com.costroom.aitoolsservice.service;

import com.costroom.aitoolsservice.dto.CreateAiToolRequest;
import com.costroom.aitoolsservice.dto.AiToolResponse;
import com.costroom.aitoolsservice.dto.UpdateAiToolRequest;
import com.costroom.aitoolsservice.entity.AiTool;
import com.costroom.aitoolsservice.entity.SupportedProvider;
import com.costroom.aitoolsservice.exception.DuplicateToolException;
import com.costroom.aitoolsservice.exception.ToolNotFoundException;
import com.costroom.aitoolsservice.exception.UnsupportedProviderException;
import com.costroom.aitoolsservice.repository.AiToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AiToolService {

    private final AiToolRepository repository;
    private final EncryptionService encryptionService;

    public AiToolService(AiToolRepository repository, EncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public AiToolResponse create(UUID userId, UUID orgId, CreateAiToolRequest req) {
        String providerSlug = validateProvider(req.aiName());
        String keyType = req.keyType() != null ? req.keyType() : "default";

        // Prevent duplicate active keys for the same provider+keyType within an org
        if (repository.existsByOrgIdAndAiNameAndKeyTypeAndActiveTrue(orgId, providerSlug, keyType)) {
            throw new DuplicateToolException(
                "An active " + providerSlug + " key of type '" + keyType +
                "' already exists for your organisation. Delete it first to replace it."
            );
        }

        AiTool tool = AiTool.builder()
                .userId(userId)
                .orgId(orgId)
                .aiName(providerSlug)
                .displayName(req.displayName())
                .encryptedKey(encryptionService.encrypt(req.apiKey()))
                .keyType(keyType)
                .active(true)
                .build();

        return AiToolResponse.from(repository.save(tool));
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AiToolResponse> listForUser(UUID userId) {
        return repository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(AiToolResponse::from)
                .toList();
    }

    // ── GET ONE ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AiToolResponse getForUser(UUID toolId, UUID userId) {
        return repository.findByIdAndUserId(toolId, userId)
                .map(AiToolResponse::from)
                .orElseThrow(() -> new ToolNotFoundException(toolId));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public AiToolResponse update(UUID toolId, UUID userId, UpdateAiToolRequest req) {
        AiTool tool = repository.findByIdAndUserId(toolId, userId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (req.displayName() != null) {
            tool.setDisplayName(req.displayName());
        }
        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            tool.setEncryptedKey(encryptionService.encrypt(req.apiKey()));
        }

        return AiToolResponse.from(repository.save(tool));
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID toolId, UUID userId) {
        AiTool tool = repository.findByIdAndUserId(toolId, userId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));
        tool.setActive(false);
        repository.save(tool);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String validateProvider(String rawName) {
        return SupportedProvider.fromSlug(rawName)
                .map(SupportedProvider::getSlug)
                .orElseThrow(() -> new UnsupportedProviderException(
                    "'" + rawName + "' is not a supported provider. " +
                    "Supported: " + SupportedProvider.validSlugs()
                ));
    }
}
