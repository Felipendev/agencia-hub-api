package com.agenciahub.api.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitacao_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_seller_id")
    private User referralSeller;

    @Column(nullable = false, length = 128)
    private String slug;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 32)
    private String telefone;

    @Column(nullable = false, columnDefinition = "text")
    private String observacoes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalhes", nullable = false, columnDefinition = "jsonb")
    private JsonNode detalhes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (observacoes == null) {
            observacoes = "";
        }
        if (email == null) {
            email = "";
        }
    }
}
