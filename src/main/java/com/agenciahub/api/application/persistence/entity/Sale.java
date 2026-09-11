package com.agenciahub.api.application.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "sales") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agency_id", nullable = false) private Agency agency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false) private CrmCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quotation_id") private Quotation quotation;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Column(name = "sale_date", nullable = false) private LocalDate saleDate;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "approval_managed", nullable = false) private boolean approvalManaged;
    @Column(name = "recurrence_frequency", length = 32) private String recurrenceFrequency;
    @Column(nullable = false, columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (notes == null) notes = ""; }
}
