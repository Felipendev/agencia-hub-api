package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="payables") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payable {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_id",nullable=false) private Sale sale;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="supplier_id") private Supplier supplier;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="commission_recipient_user_id") private PlatformAccount commissionRecipientUser;
 @Column(name="payable_type",nullable=false) private String payableType;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="due_date") private LocalDate dueDate;
 @Column(nullable=false) private String status;
 @Column(name="paid_at") private LocalDate paidAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="financial_entry_id") private FinancialEntry financialEntry;
}
