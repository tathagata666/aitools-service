package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.UserOrgLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserOrgLinkRepository extends JpaRepository<UserOrgLink, UUID> {
    Optional<UserOrgLink> findByUserId(UUID userId);
}