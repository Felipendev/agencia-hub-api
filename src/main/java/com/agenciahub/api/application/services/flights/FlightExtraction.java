package com.agenciahub.api.application.services.flights;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Untrusted extraction: missing values remain null until the agency reviews them. */
public record FlightExtraction(List<Offer> offers, List<String> warnings) {
    public record Segment(String origin, String destination, String departureDate, String arrivalDate,
                          String departureTime, String arrivalTime, Integer durationMinutes, Integer stops) {}
    public record Offer(String airline, List<Segment> segments, Long miles, BigDecimal cashAmount,
                        String priceBasis, Integer passengers, List<String> warnings) {}

    public FlightExtraction validate() {
        if (offers == null || offers.size() > 10 || warnings == null || warnings.size() > 20)
            throw new IllegalArgumentException("A leitura não retornou ofertas válidas. Preencha manualmente.");
        warnings.forEach(FlightExtraction::text);
        for (Offer offer : offers) {
            if (offer == null || offer.segments() == null || offer.segments().isEmpty() || offer.segments().size() > 8
                    || offer.warnings() == null || offer.warnings().size() > 20
                    || offer.priceBasis() == null || !List.of("PER_PERSON", "GROUP", "UNKNOWN").contains(offer.priceBasis()))
                throw new IllegalArgumentException("Formato de oferta inválido.");
            text(offer.airline()); offer.warnings().forEach(FlightExtraction::text);
            if (offer.miles() != null && (offer.miles() < 0 || offer.miles() > 100_000_000)
                    || offer.cashAmount() != null && (offer.cashAmount().signum() < 0 || offer.cashAmount().compareTo(new BigDecimal("10000000")) > 0)
                    || offer.passengers() != null && (offer.passengers() < 1 || offer.passengers() > 20))
                throw new IllegalArgumentException("Valores extraídos fora do limite.");
            for (Segment s : offer.segments()) {
                if (s == null) throw new IllegalArgumentException("Trecho extraído inválido.");
                text(s.origin()); text(s.destination());
                if (s.departureDate() != null) LocalDate.parse(s.departureDate());
                if (s.arrivalDate() != null) LocalDate.parse(s.arrivalDate());
                if (s.departureTime() != null) LocalTime.parse(s.departureTime());
                if (s.arrivalTime() != null) LocalTime.parse(s.arrivalTime());
                if (s.durationMinutes() != null && (s.durationMinutes() < 1 || s.durationMinutes() > 10080)
                        || s.stops() != null && (s.stops() < 0 || s.stops() > 20))
                    throw new IllegalArgumentException("Duração ou paradas inválidas.");
            }
        }
        return this;
    }
    private static void text(String value) {
        if (value != null && value.length() > 500) throw new IllegalArgumentException("Texto extraído excedeu o limite.");
    }
}
