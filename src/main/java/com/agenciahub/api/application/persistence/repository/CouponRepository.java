package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    List<Coupon> findByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    Optional<Coupon> findByIdAndAgency_Id(UUID id, UUID agencyId);

    Optional<Coupon> findByAgency_IdAndCodeIgnoreCase(UUID agencyId, String code);

    boolean existsByAgency_IdAndCodeIgnoreCase(UUID agencyId, String code);
}
