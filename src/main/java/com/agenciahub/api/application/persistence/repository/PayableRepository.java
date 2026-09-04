package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.Payable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface PayableRepository extends JpaRepository<Payable, UUID> {
    List<Payable> findBySale_Id(UUID saleId);
    List<Payable> findByCommissionRecipientUser_IdAndPayableTypeOrderByDueDateDesc(UUID recipientUserId, String payableType);
}
