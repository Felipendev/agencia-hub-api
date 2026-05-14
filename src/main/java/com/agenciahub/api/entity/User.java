package com.agenciahub.api.entity;

import com.agenciahub.api.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    /** Código público curto para links; preenchido pela aplicação (ver PublicLinkCodeService). */
    @Column(name = "public_link_code", length = 16)
    private String publicLinkCode;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    /** Commission as percentage (e.g. 5.00 = 5%). Mutually exclusive with commissionFixed. */
    @Column(name = "commission_pct", precision = 5, scale = 2)
    private BigDecimal commissionPct;

    /** Commission as fixed amount per approved quotation. Mutually exclusive with commissionPct. */
    @Column(name = "commission_fixed", precision = 19, scale = 2)
    private BigDecimal commissionFixed;

    @Column(length = 32)
    private String phone;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = Boolean.FALSE;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = Boolean.FALSE;

    /** Versão vigente dos termos aceita implicitamente quando true (ver TermsService). */
    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private Boolean termsAccepted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (active == null) active = Boolean.TRUE;
    }
}
