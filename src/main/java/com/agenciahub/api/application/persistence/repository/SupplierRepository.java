package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findAllByAgency_IdOrderByNameAsc(UUID agencyId);
    Optional<Supplier> findByIdAndAgency_Id(UUID id, UUID agencyId);
    boolean existsByAgency_IdAndNameIgnoreCase(UUID agencyId, String name);
}
