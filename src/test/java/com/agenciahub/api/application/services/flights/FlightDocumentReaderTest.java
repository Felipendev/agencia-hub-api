package com.agenciahub.api.application.services.flights;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FlightDocumentReaderTest {
    private static final String OFFER = "LATAM 17:25 BPS 5 h 55 min. 23:20 REC 1 parada Por pessoa a partir de 87.141 milhas + BRL 38,84 Inclui taxas e impostos";
    @Test void readsSingleTextOfferWithoutInventingDate() {
        var result = FlightDocumentReader.parseSingleOffer(OFFER);
        assertThat(result).isNotNull();
        var offer = result.offers().get(0);
        assertThat(offer.miles()).isEqualTo(87141L);
        assertThat(offer.cashAmount()).isEqualByComparingTo("38.84");
        assertThat(offer.segments().get(0).departureDate()).isNull();
        assertThat(offer.segments().get(0).durationMinutes()).isEqualTo(355);
        assertThat(offer.segments().get(0).stops()).isEqualTo(1);
    }
    @Test void ambiguousOrMultipleOffersRequireInterpretation() {
        assertThat(FlightDocumentReader.parseSingleOffer(OFFER + "\n" + OFFER)).isNull();
        assertThat(FlightDocumentReader.parseSingleOffer(OFFER.replace("Por pessoa", "Total do grupo"))).isNull();
        assertThat(FlightDocumentReader.parseSingleOffer("20/09/2026 " + OFFER)).isNull();
    }
    @Test void rejectsUnsupportedContentAndOversizeBeforeProvider() {
        var reader = new FlightDocumentReader();
        assertThatThrownBy(() -> reader.read("<html>not an image</html>".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.read(new byte[4 * 1024 * 1024 + 1])).isInstanceOf(IllegalArgumentException.class);
    }
}
