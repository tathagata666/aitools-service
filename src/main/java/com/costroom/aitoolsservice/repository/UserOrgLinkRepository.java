package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.UserOrgLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOrgLinkRepository extends JpaRepository<UserOrgLink, UUID> {

    Optional<UserOrgLink> findByUserId(UUID userId);
}