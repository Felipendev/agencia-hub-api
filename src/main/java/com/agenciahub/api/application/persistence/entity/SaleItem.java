package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="sale_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleItem {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_id",nullable=false) private Sale sale;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="supplier_id") private Supplier supplier;
 @Column(nullable=false) private String description;
 @Column(name="item_type",nullable=false) private String itemType;
 @Column(name="sale_amount",nullable=false,precision=19,scale=2) private BigDecimal saleAmount;
 @Column(name="supplier_cost",precision=19,scale=2) private BigDecimal supplierCost;
 @Column(name="customer_pays_supplier_directly",nullable=false) private Boolean customerPaysSupplierDirectly;
}
