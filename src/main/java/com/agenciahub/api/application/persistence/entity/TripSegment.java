package com.agenciahub.api.application.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Trecho de voo (0..N por viagem): suporta ida/volta e conexões. */
@Entity @Table(name = "trip_segments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripSegment {
 @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id", nullable = false) private Trip trip;
 @Column(name = "segment_number", nullable = false) private Integer segmentNumber;
 @Column(length = 128) private String origin;
 @Column(length = 128) private String destination;
 @Column(name = "departure_at") private Instant departureAt;
 @Column(name = "arrival_at") private Instant arrivalAt;
 @Column(length = 128) private String airline;
 @Column(name = "flight_number", length = 32) private String flightNumber;
 @Column(name = "ticket_number", length = 64) private String ticketNumber;
}
