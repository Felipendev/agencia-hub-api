package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    List<Sale> findByQuotation_IdOrderByCreatedAtAsc(UUID quotationId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Sale s where s.id = :id and s.agency.id = :agencyId")
    Optional<Sale> findForUpdate(UUID id, UUID agencyId);
    List<Sale> findAllByAgency_IdOrderBySaleDateDesc(UUID agencyId);
    List<Sale> findAllByAgency_IdAndCustomer_IdOrderBySaleDateDesc(UUID agencyId, UUID customerId);
    Optional<Sale> findByIdAndAgency_Id(UUID id, UUID agencyId);
}
