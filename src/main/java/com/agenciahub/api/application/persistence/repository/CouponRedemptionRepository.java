package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
    boolean existsByCoupon_IdAndCustomerEmailIgnoreCase(UUID couponId, String customerEmail);
}
