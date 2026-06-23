package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    @Query("SELECT ui.userId FROM UserIdentity ui WHERE ui.providerSubject = :sub")
    Optional<UUID> findUserIdByCognitoSub(@Param("sub") String sub);
}