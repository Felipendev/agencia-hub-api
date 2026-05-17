package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
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

    List<PlatformAccount> findByAccountKindAndActiveTrue(AccountKind accountKind);

    List<PlatformAccount> findByAgency_IdAndAccountKindAndActiveTrue(UUID agencyId, AccountKind accountKind);
    List<PlatformAccount> findAllByOrderByNameAsc();

    List<PlatformAccount> findByAgency_Id(UUID agencyId);

    long countByAgency_IdAndAccountKind(UUID agencyId, AccountKind accountKind);
}
