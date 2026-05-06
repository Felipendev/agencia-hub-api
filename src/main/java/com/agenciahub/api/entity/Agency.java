package com.agenciahub.api.entity;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 32)
    private String phone;

    @Column(name = "logo_url", length = 1024)
    private String logoUrl;

    @Column(length = 18)
    private String cnpj;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "commercial_email", length = 320)
    private String commercialEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgencyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 32)
    private SubscriptionStatus subscriptionStatus;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
