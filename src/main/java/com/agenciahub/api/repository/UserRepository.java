package com.agenciahub.api.repository;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleAndActiveTrue(UserRole role);
    List<User> findAllByOrderByNameAsc();
}
