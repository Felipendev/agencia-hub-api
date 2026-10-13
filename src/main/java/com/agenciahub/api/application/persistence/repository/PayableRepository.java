package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Payable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface PayableRepository extends JpaRepository<Payable, UUID> {
    List<Payable> findBySale_Id(UUID saleId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payable p where p.id=:id")
    java.util.Optional<Payable> findForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    List<Payable> findByCommissionRecipientUser_IdAndPayableTypeOrderByDueDateDesc(UUID recipientUserId, String payableType);
}
