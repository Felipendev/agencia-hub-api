package com.agenciahub.api.application.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "agency_attachments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgencyAttachment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agency_id", nullable = false) private Agency agency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_id") private Sale sale;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "financial_entry_id") private FinancialEntry financialEntry;
    @Column(name = "original_filename", nullable = false) private String originalFilename;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    // Hibernate 6 mapeia byte[] para OID (large object) por padrão no dialeto Postgres;
    // a coluna é BYTEA (migração V36), então força VARBINARY para casar com o schema real.
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(nullable = false) private byte[] content;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = Instant.now(); }
}
