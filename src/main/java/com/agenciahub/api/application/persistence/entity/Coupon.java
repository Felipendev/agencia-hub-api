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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Cupom de desconto cadastrado pelo dono da agência para uso no formulário público. See V43/V44. */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agency_id", nullable = false) private Agency agency;
    @Column(nullable = false, length = 40) private String code;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    /** Desconto percentual (0–100]; null = cupom ainda sem desconto configurado (aceita/recusa apenas). */
    @Column(name = "discount_percent", precision = 5, scale = 2) private BigDecimal discountPercent;
    /** Teto opcional do desconto em R$ quando o percentual, aplicado ao valor da venda, ultrapassaria esse valor. */
    @Column(name = "max_discount_amount", precision = 19, scale = 2) private BigDecimal maxDiscountAmount;
    /** Limite total de resgates (qualquer cliente); null = sem limite. */
    @Column(name = "max_uses") private Integer maxUses;
    @Column(name = "used_count", nullable = false) private int usedCount;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (code != null) code = code.trim().toUpperCase();
    }

    /** Válido no sentido amplo (data + limite total) — não checa se um cliente específico já usou; ver CouponRedemptionRepository. */
    public boolean isValidNow() {
        return active
                && (expiresAt == null || expiresAt.isAfter(Instant.now()))
                && (maxUses == null || usedCount < maxUses);
    }
}
