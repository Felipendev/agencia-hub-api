package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
    List<SaleItem> findBySale_Id(UUID saleId);
    List<SaleItem> findBySale_IdInOrderByDescriptionAsc(java.util.Collection<UUID> saleIds);
}
