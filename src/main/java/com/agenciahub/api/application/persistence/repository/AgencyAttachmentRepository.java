package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.AgencyAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AgencyAttachmentRepository extends JpaRepository<AgencyAttachment, UUID> {
    List<AgencyAttachment> findBySale_IdAndAgency_IdOrderByCreatedAtDesc(UUID saleId, UUID agencyId);
    List<AgencyAttachment> findByFinancialEntry_IdAndAgency_IdOrderByCreatedAtDesc(UUID financialEntryId, UUID agencyId);
    Optional<AgencyAttachment> findByIdAndAgency_Id(UUID id, UUID agencyId);
}
