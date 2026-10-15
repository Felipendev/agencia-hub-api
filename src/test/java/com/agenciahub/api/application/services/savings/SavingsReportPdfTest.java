package com.agenciahub.api.application.services.savings;

import com.agenciahub.api.application.services.savings.SavingsReportService.Content;
import com.agenciahub.api.application.services.savings.SavingsReportService.Cost;
import com.agenciahub.api.application.services.savings.SavingsReportService.Line;
import com.agenciahub.api.application.services.savings.TripSavingsService.Totals;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsReportPdfTest {

    private static Line line(boolean available) {
        return new Line("AEREO", "LATAM", null, "COMPLETED", LocalDate.of(2026, 3, 10), null, available,
                available ? List.of() : List.of("Falta a metodologia"), new BigDecimal("5000.00"), "AIRLINE_SITE", null,
                LocalDate.of(2026, 2, 1), "FOR-LIS", 2, "Econômica", "1 mala", null, "NOT_USED",
                List.of(new Cost("TAXES", new BigDecimal("400.00"), "Taxas de embarque", null)), List.of(),
                new BigDecimal("3550.50"), available ? new BigDecimal("1449.50") : null, available ? new BigDecimal("29.0") : null,
                available ? "Mesma rota" : null, null, LocalDate.of(2026, 3, 10), null);
    }

    @Test
    void rendersTotalsLinesAndUnavailableReasonsAcrossPages() throws Exception {
        List<Line> lines = new java.util.ArrayList<>(Collections.nCopies(40, line(true)));
        lines.add(line(false));
        Content content = new Content("Resultado 2026", "Cliente", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, lines,
                new Totals(41, 40, 1, 0, new BigDecimal("200000.00"), new BigDecimal("142020.00"), new BigDecimal("57980.00"), new BigDecimal("29.0")),
                LocalDate.of(2026, 10, 14), null);
        try (var document = Loader.loadPDF(new SavingsReportPdf().render(content))) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Resultado 2026", "Economia realizada: R$ 57.980,00", "Custo — Taxas: R$ 400,00", "Falta a metodologia", "Página 1 de");
        }
    }

    @Test
    void reportWithoutDocumentedTripsSaysUnavailable() throws Exception {
        Content content = new Content("Relatório", "Ana", null, null, "Mensagem 🚀", List.of(line(false)),
                new Totals(1, 0, 1, 0, null, null, null, null), LocalDate.of(2026, 10, 14), null);
        try (var document = Loader.loadPDF(new SavingsReportPdf().render(content))) {
            assertThat(new PDFTextStripper().getText(document)).contains("Economia realizada indisponível", "Mensagem ?", "todas as viagens");
        }
    }
}
