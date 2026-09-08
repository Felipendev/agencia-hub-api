package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;

/** Base própria de aeroportos (IATA) para autocomplete de origem/destino — ver migração V39. */
@Entity @Table(name = "airports") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Airport {
 @Id @Column(name = "iata_code", length = 3) private String iataCode;
 @Column(nullable = false) private String name;
 @Column(nullable = false) private String city;
 @Column(nullable = false, length = 120) private String country;
}
