package com.agenciahub.api.application.services.contract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractPdfRendererTest {

    private final ContractPdfRenderer renderer = new ContractPdfRenderer();

    @Test
    void rendersAccentsPaginatesLongContentAndReplacesUnsupportedCharacters() throws Exception {
        String content = "Cláusula 1ª — Contratação de assessoria em milhas.\n" + "Linha longa de conteúdo ".repeat(400) + "\nEmoji 🚀 fim";
        byte[] bytes = renderer.render("Contrato de Prestação", content, "Documento v1 · SHA-256 abc");
        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Contrato de Prestação").contains("Cláusula 1ª").contains("Emoji ? fim")
                    .contains("Documento v1").contains("Página 1 de " + document.getNumberOfPages());
        }
    }

    @Test
    void emptyContentStillProducesOnePage() throws Exception {
        try (PDDocument document = Loader.loadPDF(renderer.render("T", "", "rodapé"))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void safeFilenameRemovesAccentsAndSymbols() {
        assertThat(ContractService.safeFilename("Contrato — Assessoria ÁÉ #1")).isEqualTo("contrato-assessoria-ae-1");
        assertThat(ContractService.safeFilename("***")).isEqualTo("contrato");
    }
}
