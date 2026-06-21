package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}