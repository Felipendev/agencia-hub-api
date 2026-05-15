package com.agenciahub.api.repository;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.entity.PlatformAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, UUID> {
    @EntityGraph(attributePaths = "agency")
    Optional<PlatformAccount> findByEmail(String email);

    @EntityGraph(attributePaths = "agency")
    @Query("SELECT u FROM PlatformAccount u WHERE u.id = :id")
    Optional<PlatformAccount> findByIdWithAgency(@Param("id") UUID id);

    boolean existsByEmail(String email);

    boolean existsByPublicLinkCode(String publicLinkCode);

    @EntityGraph(attributePaths = "agency")
    Optional<PlatformAccount> findByPublicLinkCode(String publicLinkCode);

    List<PlatformAccount> findByRoleAndActiveTrue(AccountKind role);
    List<PlatformAccount> findAllByOrderByNameAsc();
}
