package com.agenciahub.api.repository;

import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByOrderByCreatedAtDesc();

    List<Customer> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String namePart);

    List<Customer> findByStatusOrderByCreatedAtDesc(CustomerStatus status);

    List<Customer> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String namePart, CustomerStatus status);

    // ── Soft-delete queries ───────────────────────────────────────────────────

    List<Customer> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Customer> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Customer> findByIdAndDeletedAtIsNotNull(UUID id);

    List<Customer> findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Customer> findByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(CustomerStatus status);

    List<Customer> findByDeletedAtIsNullAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String name, CustomerStatus status);

    // ── Multi-tenancy (agency_id filtering) ──────────────────────────────────

    List<Customer> findByAgency_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID agencyId);

    List<Customer> findByAgency_IdAndDeletedAtIsNotNullOrderByDeletedAtDesc(UUID agencyId);

    List<Customer> findByAgency_IdAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID agencyId, String name);

    List<Customer> findByAgency_IdAndDeletedAtIsNullAndStatusOrderByCreatedAtDesc(
            UUID agencyId, CustomerStatus status);

    List<Customer> findByAgency_IdAndDeletedAtIsNullAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID agencyId, String name, CustomerStatus status);

    // ── Verificações de unicidade ─────────────────────────────────────────────

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    boolean existsByNormalizedPhone(@Param("phone") String normalizedPhone);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone AND c.id <> :id")
    boolean existsByNormalizedPhoneAndIdNot(@Param("phone") String normalizedPhone, @Param("id") UUID id);
}
