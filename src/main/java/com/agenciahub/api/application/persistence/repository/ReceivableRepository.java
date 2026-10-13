package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {
    List<Receivable> findBySale_Id(UUID saleId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Receivable r where r.id=:id")
    java.util.Optional<Receivable> findForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    List<Receivable> findBySale_Agency_IdOrderByDueDateDesc(UUID agencyId);
}
