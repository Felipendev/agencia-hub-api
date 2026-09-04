package com.agenciahub.api.application.persistence.repository;
import com.agenciahub.api.application.persistence.entity.SaleCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface SaleCommissionRepository extends JpaRepository<SaleCommission, UUID> { List<SaleCommission> findBySale_Id(UUID saleId); }
