package com.agenciahub.api.application.services.flights;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

@Component
public class FlightDocumentReader {
    public record Document(String mime, byte[] bytes, String text, FlightExtraction direct) {}

    public Document read(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > 4 * 1024 * 1024)
            throw new IllegalArgumentException("Envie um arquivo de até 4 MB.");
        String signature = new String(bytes, 0, Math.min(bytes.length, 12), StandardCharsets.ISO_8859_1);
        if (signature.startsWith("%PDF-")) {
            try (var pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted() || !pdf.getCurrentAccessPermission().canExtractContent())
                    throw new IllegalArgumentException("Envie um PDF sem senha e com extração permitida.");
                if (pdf.getNumberOfPages() < 1 || pdf.getNumberOfPages() > 5)
                    throw new IllegalArgumentException("O PDF deve ter de 1 a 5 páginas.");
                var stripper = new PDFTextStripper(); stripper.setSortByPosition(true);
                StringBuilder text = new StringBuilder();
                boolean imageContent = false;
                for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                    stripper.setStartPage(i); stripper.setEndPage(i);
                    String page = stripper.getText(pdf);
                    if (page.length() > 12000) throw new IllegalArgumentException("PDF contém texto demais. Envie apenas as ofertas.");
                    text.append("\nPágina ").append(i).append(":\n").append(page);
                    // An image on a text page can contain another offer; send the whole PDF in that case.
                    var resources = pdf.getPage(i - 1).getResources();
                    if (page.strip().length() < 30) imageContent = true;
                    if (resources != null && resources.getXObjectNames().iterator().hasNext()) imageContent = true;
                }
                String value = text.toString();
                return new Document("application/pdf", bytes, imageContent ? null : value,
                        imageContent ? null : parseSingleOffer(value));
            }
        }
        String mime = signature.startsWith("\u0089PNG") ? "image/png"
                : (bytes[0] & 255) == 255 && bytes.length > 2 && (bytes[1] & 255) == 216 ? "image/jpeg" : null;
        if (mime == null) throw new IllegalArgumentException("Formatos aceitos: PDF, PNG e JPEG.");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Imagem inválida.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > 12000 || h > 12000 || (long) w * h > 20_000_000)
                    throw new IllegalArgumentException("Imagem muito grande. Recorte a região das ofertas.");
            } finally { reader.dispose(); }
        }
        return new Document(mime, bytes, null, null);
    }

    /** Deliberately narrow: only one clearly delimited LATAM-style offer is mapped without AI. */
    public static FlightExtraction parseSingleOffer(String text) {
        var miles = Pattern.compile("(?i)([0-9][0-9.]*)\\s+milhas").matcher(text);
        if (!miles.find()) return null;
        long amount;
        try { amount = Long.parseLong(miles.group(1).replace(".", "")); } catch (NumberFormatException e) { return null; }
        if (miles.find()) return null;
        var airports = Pattern.compile("\\b([0-2][0-9]:[0-5][0-9])\\s+([A-Z]{3})\\b").matcher(text);
        if (!airports.find()) return null;
        String departure = airports.group(1), origin = airports.group(2);
        if (!airports.find()) return null;
        String arrival = airports.group(1), destination = airports.group(2);
        if (airports.find()) return null;
        var cash = Pattern.compile("(?:BRL|R\\$)\\s*([0-9.]+,[0-9]{2})").matcher(text);
        if (!cash.find()) return null;
        BigDecimal money = new BigDecimal(cash.group(1).replace(".", "").replace(',', '.'));
        if (cash.find()) return null;
        var duration = Pattern.compile("(?i)(\\d{1,2})\\s*h\\s*(\\d{1,2})\\s*(?:min|m)").matcher(text);
        if (!duration.find()) return null;
        int minutes = Integer.parseInt(duration.group(1)) * 60 + Integer.parseInt(duration.group(2));
        var stops = Pattern.compile("(?i)(\\d+)\\s+parada").matcher(text);
        Integer count = stops.find() ? Integer.valueOf(stops.group(1)) : null;
        if (!text.toLowerCase(Locale.ROOT).contains("por pessoa")) return null;
        // Dates need interpretation if present; don't silently discard dates from a full PDF.
        if (Pattern.compile("\\d{1,4}[-/]\\d{1,2}[-/]\\d{1,4}|(?i)janeiro|fevereiro|março|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro").matcher(text).find()) return null;
        return new FlightExtraction(List.of(new FlightExtraction.Offer(
                text.toUpperCase(Locale.ROOT).contains("LATAM") ? "LATAM" : null,
                List.of(new FlightExtraction.Segment(origin, destination, null, null, departure, arrival, minutes, count)),
                amount, money, "PER_PERSON", null,
                List.of("Confira as datas, a tarifa a partir de e se a parcela em dinheiro já inclui as taxas."))),
                List.of("Extração direta de texto. Confira os dados antes de aplicar.")).validate();
    }
}
