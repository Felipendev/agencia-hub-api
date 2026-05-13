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

    List<Customer> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Customer> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Customer> findByIdAndDeletedAtIsNotNull(UUID id);

    List<Customer> findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Customer> findByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(CustomerStatus status);

    List<Customer> findByDeletedAtIsNullAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String name, CustomerStatus status);

    boolean existsByDeletedAtIsNullAndEmailIgnoreCase(String email);

    boolean existsByDeletedAtIsNullAndEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.deletedAt IS NULL AND FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    boolean existsByNormalizedPhone(@Param("phone") String normalizedPhone);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.deletedAt IS NULL AND FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone AND c.id <> :id")
    boolean existsByNormalizedPhoneAndIdNot(@Param("phone") String normalizedPhone, @Param("id") UUID id);

    Optional<Customer> findFirstByDeletedAtIsNullAndEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    Optional<Customer> findFirstActiveByNormalizedPhone(@Param("phone") String phone);
}