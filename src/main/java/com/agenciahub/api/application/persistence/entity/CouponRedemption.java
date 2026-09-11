package com.agenciahub.api.application.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Um cupom só pode ser resgatado uma vez pelo mesmo e-mail de cliente. See V44. */
@Entity
@Table(name = "coupon_redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRedemption {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coupon_id", nullable = false) private Coupon coupon;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agency_id", nullable = false) private Agency agency;
    @Column(name = "customer_email", nullable = false, length = 320) private String customerEmail;
    @Column(name = "submission_id") private UUID submissionId;
    @Column(name = "redeemed_at", nullable = false) private Instant redeemedAt;

    @PrePersist
    void prePersist() {
        if (redeemedAt == null) redeemedAt = Instant.now();
        if (customerEmail != null) customerEmail = customerEmail.trim().toLowerCase();
    }
}
