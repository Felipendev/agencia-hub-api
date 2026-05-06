package com.agenciahub.api.entity;

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

@Entity
@Table(name = "terms_acceptances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "terms_version", nullable = false, length = 32)
    private String termsVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @PrePersist
    void prePersist() {
        if (acceptedAt == null) {
            acceptedAt = Instant.now();
        }
    }
}
