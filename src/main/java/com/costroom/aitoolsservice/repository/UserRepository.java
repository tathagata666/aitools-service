package com.costroom.aitoolsservice.repository;

import com.costroom.aitoolsservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}