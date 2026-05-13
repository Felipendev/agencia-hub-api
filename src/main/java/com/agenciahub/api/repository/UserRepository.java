package com.agenciahub.api.repository;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "agency")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "agency")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAgency(@Param("id") UUID id);

    boolean existsByEmail(String email);

    boolean existsByPublicLinkCode(String publicLinkCode);

    @EntityGraph(attributePaths = "agency")
    Optional<User> findByPublicLinkCode(String publicLinkCode);

    List<User> findByRoleAndActiveTrue(UserRole role);
    List<User> findAllByOrderByNameAsc();
}
