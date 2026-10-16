package com.agenciahub.api.application.services.savings;

import com.agenciahub.api.application.services.savings.SavingsReportService.Content;
import com.agenciahub.api.application.services.savings.SavingsReportService.Line;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** PDF do retrato do relatório; gerado a partir do conteúdo gravado, então não muda com edições posteriores. */
@Component
public class SavingsReportPdf {

    private static final float MARGIN = 50f;
    private static final float SIZE = 10f;
    private static final float LEADING = SIZE * 1.45f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> COST = Map.of("AGENCY_PAYMENT", "Pago à agência", "DIRECT_SUPPLIER_PAYMENT", "Pago ao fornecedor",
            "TAXES", "Taxas", "SERVICE_FEE", "Honorários/taxa de serviço", "MILES_ACQUISITION", "Aquisição de milhas", "OTHER", "Outro");
    private static final Map<String, String> ADJUSTMENT = Map.of("REFUND", "Reembolso (-)", "OTHER_CREDIT", "Crédito (-)",
            "CANCELLATION_FEE", "Multa de cancelamento (+)", "CHANGE_FEE", "Taxa de remarcação (+)", "OTHER_CHARGE", "Outra cobrança (+)");

    record Text(String value, boolean bold) {}

    public byte[] render(Content content) {
        List<Text> lines = lines(content);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDRectangle size = PDRectangle.A4;
        float width = size.getWidth() - 2 * MARGIN;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.getDocumentInformation().setTitle(sanitize(regular, content.title()));
            List<Text> wrapped = new ArrayList<>();
            for (Text line : lines) {
                PDType1Font font = line.bold() ? bold : regular;
                for (String part : wrap(font, sanitize(font, line.value()), width)) wrapped.add(new Text(part, line.bold()));
            }
            int perPage = (int) ((size.getHeight() - 2 * MARGIN) / LEADING);
            int pages = Math.max(1, (wrapped.size() + perPage - 1) / perPage);
            for (int page = 0; page < pages; page++) {
                PDPage pdPage = new PDPage(size);
                document.addPage(pdPage);
                try (PDPageContentStream stream = new PDPageContentStream(document, pdPage)) {
                    float y = size.getHeight() - MARGIN;
                    for (Text line : wrapped.subList(page * perPage, Math.min(wrapped.size(), (page + 1) * perPage))) {
                        if (!line.value().isEmpty()) write(stream, line.bold() ? bold : regular, MARGIN, y, line.value());
                        y -= LEADING;
                    }
                    write(stream, regular, MARGIN, MARGIN / 2, "Página " + (page + 1) + " de " + pages);
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o PDF do relatório.", e);
        }
    }

    static List<Text> lines(Content c) {
        List<Text> out = new ArrayList<>();
        out.add(new Text(c.title(), true));
        out.add(new Text(c.customerLabel() + " · Período: " + period(c.from(), c.to()) + " · Gerado em " + date(c.generatedOn()), false));
        if (c.message() != null) out.add(new Text(c.message(), false));
        out.add(new Text("", false));
        var t = c.totals();
        out.add(new Text("Resumo", true));
        out.add(new Text("Viagens no período: " + t.trips() + " · com economia documentada: " + t.available()
                + " · indisponíveis: " + t.unavailable() + " · canceladas: " + t.cancelled(), false));
        if (t.savings() != null) {
            out.add(new Text("Preço comparável: " + money(t.comparablePrice()) + " · Custo efetivo: " + money(t.effectiveCost())
                    + " · Economia realizada: " + money(t.savings()) + (t.savingsPercent() == null ? "" : " (" + t.savingsPercent() + "%)"), false));
        } else {
            out.add(new Text("Economia realizada indisponível: nenhuma viagem do período tem comparação documentada.", false));
        }
        out.add(new Text("Somente viagens com comparação equivalente e custo documentado entram no total. Não inclui lucro da agência nem estimativas de cotação.", false));
        for (Line line : c.lines()) {
            out.add(new Text("", false));
            out.add(new Text(join(" · ", line.service(), line.airline(), line.locator() == null ? null : "Localizador " + line.locator(),
                    line.travelStartDate() == null ? "Sem data" : date(line.travelStartDate()) + (line.travelEndDate() == null ? "" : " a " + date(line.travelEndDate()))), true));
            if (!line.available()) {
                out.add(new Text("Economia indisponível: " + String.join("; ", line.unavailableReasons()), false));
            } else {
                out.add(new Text("Economia: " + money(line.savings()) + " (" + line.savingsPercent() + "%) = preço comparável " + money(line.comparablePrice())
                        + " - custo efetivo " + money(line.effectiveCost()), false));
            }
            if (line.comparablePrice() != null) {
                out.add(new Text("Comparativo: " + join(" · ", line.route(), line.passengers() == null ? null : line.passengers() + " passageiro(s)",
                        line.fareConditions(), line.baggage() == null ? null : "Bagagem: " + line.baggage(),
                        line.researchedOn() == null ? null : "pesquisado em " + date(line.researchedOn()), line.source()), false));
            }
            if (line.differences() != null) out.add(new Text("Diferenças registradas: " + line.differences(), false));
            if (line.comparisonOutboundDate() != null) out.add(new Text("Datas do comparativo: " + date(line.comparisonOutboundDate())
                    + (line.comparisonReturnDate() == null ? " (somente ida)" : " a " + date(line.comparisonReturnDate())), false));
            for (var cost : line.costs()) {
                out.add(new Text("  Custo — " + COST.getOrDefault(cost.kind(), cost.kind()) + ": " + money(cost.amount()) + " · " + cost.description()
                        + (cost.origin() == null ? "" : " (origem: " + cost.origin() + ")"), false));
            }
            for (var adj : line.adjustments()) {
                out.add(new Text("  Ajuste — " + ADJUSTMENT.getOrDefault(adj.kind(), adj.kind()) + ": " + money(adj.amount()) + " em "
                        + date(adj.date()) + " · " + adj.description(), false));
            }
            if (line.methodology() != null) out.add(new Text("Metodologia: " + line.methodology(), false));
            if (line.notes() != null) out.add(new Text("Observações: " + line.notes(), false));
        }
        return out;
    }

    private static String period(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "todas as viagens";
        return (from == null ? "início" : date(from)) + " a " + (to == null ? "hoje" : date(to));
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value).replace(' ', ' ');
    }

    private static String date(LocalDate date) { return date == null ? "—" : DATE.format(date); }

    private static String join(String sep, String... parts) {
        return String.join(sep, java.util.Arrays.stream(parts).filter(p -> p != null && !p.isBlank()).toList());
    }

    private static void write(PDPageContentStream stream, PDType1Font font, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, SIZE);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private static List<String> wrap(PDType1Font font, String text, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text.isBlank()) { lines.add(""); return lines; }
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000f * SIZE <= width) { line.setLength(0); line.append(candidate); continue; }
            if (!line.isEmpty()) lines.add(line.toString());
            line.setLength(0);
            String rest = word;
            while (font.getStringWidth(rest) / 1000f * SIZE > width && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && font.getStringWidth(rest.substring(0, cut)) / 1000f * SIZE > width) cut--;
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        lines.add(line.toString());
        return lines;
    }

    /** Fontes padrão usam WinAnsi; caracteres fora dele viram "?" em vez de falhar. */
    private static String sanitize(PDType1Font font, String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (Character.isISOControl(cp)) { out.append(' '); return; }
            String ch = new String(Character.toChars(cp));
            try { font.encode(ch); out.append(ch); } catch (IOException | IllegalArgumentException e) { out.append('?'); }
        });
        return out.toString();
    }
}
