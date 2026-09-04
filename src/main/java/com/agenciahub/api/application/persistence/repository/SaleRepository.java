package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    List<Sale> findAllByAgency_IdOrderBySaleDateDesc(UUID agencyId);
    List<Sale> findAllByAgency_IdAndCustomer_IdOrderBySaleDateDesc(UUID agencyId, UUID customerId);
    Optional<Sale> findByIdAndAgency_Id(UUID id, UUID agencyId);
}
