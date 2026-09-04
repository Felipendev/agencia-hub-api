package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name="sale_commissions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleCommission {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_id",nullable=false) private Sale sale;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recipient_user_id",nullable=false) private PlatformAccount recipientUser;
 @Column(name="calculation_type",nullable=false) private String calculationType;
 @Column(name="calculation_value",nullable=false,precision=19,scale=4) private BigDecimal calculationValue;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
}
