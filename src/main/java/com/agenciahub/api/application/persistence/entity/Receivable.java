package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="receivables") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Receivable {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_id",nullable=false) private Sale sale;
 @Column(name="installment_number",nullable=false) private Integer installmentNumber;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="due_date",nullable=false) private LocalDate dueDate;
 @Column(nullable=false) private String status;
 @Column(name="payment_method",length=32) private String paymentMethod;
 @Column(name="bank_account",length=128) private String bankAccount;
 @Column(name="received_at") private LocalDate receivedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="financial_entry_id") private FinancialEntry financialEntry;
}
