package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {
    List<Receivable> findBySale_Id(UUID saleId);
    List<Receivable> findBySale_Agency_IdOrderByDueDateDesc(UUID agencyId);
}
