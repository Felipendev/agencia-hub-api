package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Venda/viagem emitida, com localizador e itinerário; opcionalmente ligada a cotação e venda. */
@Entity @Table(name = "trips") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trip {
 @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agency_id", nullable = false) private Agency agency;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false) private CrmCustomer customer;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quotation_id") private Quotation quotation;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_id") private Sale sale;
 @Column(name = "service_type", nullable = false, length = 32) private String serviceType;
 @Column(name = "booking_locator", length = 64) private String bookingLocator;
 @Column(length = 128) private String airline;
 @Column(nullable = false, length = 32) private String status;
 @Column(name = "sale_date") private LocalDate saleDate;
 @Column(name = "travel_start_date") private LocalDate travelStartDate;
 @Column(name = "travel_end_date") private LocalDate travelEndDate;
 @Column(nullable = false, columnDefinition = "text") private String notes;
 @Column(name = "created_at", nullable = false) private Instant createdAt;
 @Column(name = "updated_at", nullable = false) private Instant updatedAt;
 @PrePersist void prePersist() { Instant now = Instant.now(); createdAt = now; updatedAt = now; if (notes == null) notes = ""; if (status == null) status = "UPCOMING"; }
 @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
