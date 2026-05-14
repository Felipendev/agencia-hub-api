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

    List<Customer> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Customer> findByStatusOrderByCreatedAtDesc(CustomerStatus status);

    List<Customer> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String name, CustomerStatus status);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    boolean existsByNormalizedPhone(@Param("phone") String normalizedPhone);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone AND c.id <> :id")
    boolean existsByNormalizedPhoneAndIdNot(@Param("phone") String normalizedPhone, @Param("id") UUID id);

    Optional<Customer> findFirstByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    Optional<Customer> findFirstByNormalizedPhone(@Param("phone") String phone);
}
