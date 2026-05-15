package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmCustomerRepository extends JpaRepository<CrmCustomer, UUID> {

    List<CrmCustomer> findAllByOrderByCreatedAtDesc();

    List<CrmCustomer> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<CrmCustomer> findByStatusOrderByCreatedAtDesc(CustomerStatus status);

    List<CrmCustomer> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String name, CustomerStatus status);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("SELECT COUNT(c) > 0 FROM CrmCustomer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    boolean existsByNormalizedPhone(@Param("phone") String normalizedPhone);

    @Query("SELECT COUNT(c) > 0 FROM CrmCustomer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone AND c.id <> :id")
    boolean existsByNormalizedPhoneAndIdNot(@Param("phone") String normalizedPhone, @Param("id") UUID id);

    Optional<CrmCustomer> findFirstByEmailIgnoreCase(String email);

    @Query("SELECT c FROM CrmCustomer c WHERE FUNCTION('regexp_replace', c.phone, '\\D', '', 'g') = :phone")
    Optional<CrmCustomer> findFirstByNormalizedPhone(@Param("phone") String phone);
}
