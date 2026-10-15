package com.agenciahub.api.application.services.contract;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o PDF de um documento já emitido. O conteúdo vem do texto imutável gravado na emissão,
 * então o PDF não muda quando a pessoa ou o modelo são alterados depois.
 */
@Component
public class ContractPdfRenderer {

    private static final float MARGIN = 56f;
    private static final float BODY_SIZE = 10.5f;
    private static final float TITLE_SIZE = 14f;
    private static final float FOOTER_SIZE = 7.5f;
    private static final float LEADING = BODY_SIZE * 1.45f;

    public byte[] render(String title, String content, String footer) {
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDRectangle size = PDRectangle.A4;
        float width = size.getWidth() - 2 * MARGIN;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.getDocumentInformation().setTitle(sanitize(regular, title));
            List<String> titleLines = wrap(bold, TITLE_SIZE, sanitize(bold, title), width);
            List<String> lines = new ArrayList<>();
            for (String paragraph : content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
                lines.addAll(wrap(regular, BODY_SIZE, sanitize(regular, paragraph), width));
            }
            String footerText = sanitize(regular, footer);

            List<List<String>> pages = paginate(titleLines.size(), lines, size.getHeight());
            int pageNumber = 0;
            for (List<String> pageLines : pages) {
                pageNumber++;
                PDPage page = new PDPage(size);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float y = size.getHeight() - MARGIN;
                    if (pageNumber == 1) {
                        for (String line : titleLines) {
                            write(stream, bold, TITLE_SIZE, MARGIN, y, line);
                            y -= TITLE_SIZE * 1.4f;
                        }
                        y -= LEADING;
                    }
                    for (String line : pageLines) {
                        if (!line.isEmpty()) write(stream, regular, BODY_SIZE, MARGIN, y, line);
                        y -= LEADING;
                    }
                    String pageFooter = footerText + " · Página " + pageNumber + " de " + pages.size();
                    String footerLine = wrap(regular, FOOTER_SIZE, pageFooter, width).get(0);
                    write(stream, regular, FOOTER_SIZE, MARGIN, MARGIN / 2, footerLine);
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o PDF do contrato.", e);
        }
    }

    private static List<List<String>> paginate(int titleLines, List<String> lines, float pageHeight) {
        float usable = pageHeight - 2 * MARGIN;
        int firstCapacity = Math.max(1, (int) ((usable - titleLines * TITLE_SIZE * 1.4f - LEADING) / LEADING));
        int capacity = Math.max(1, (int) (usable / LEADING));
        List<List<String>> pages = new ArrayList<>();
        int index = 0;
        int current = firstCapacity;
        do {
            int end = Math.min(lines.size(), index + current);
            pages.add(lines.subList(index, end));
            index = end;
            current = capacity;
        } while (index < lines.size());
        return pages;
    }

    private static void write(PDPageContentStream stream, PDType1Font font, float fontSize, float x, float y, String text)
            throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    static List<String> wrap(PDType1Font font, float fontSize, String paragraph, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        if (paragraph.isBlank()) {
            lines.add("");
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split(" ", -1)) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(font, fontSize, candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            // Palavra maior que a linha: quebra por caractere.
            String rest = word;
            while (width(font, fontSize, rest) > width) {
                int cut = rest.length() - 1;
                while (cut > 1 && width(font, fontSize, rest.substring(0, cut)) > width) cut--;
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        lines.add(line.toString());
        return lines;
    }

    private static float width(PDType1Font font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    /** Fontes padrão do PDF usam WinAnsi; caracteres fora dele viram "?" em vez de falhar a geração. */
    static String sanitize(PDType1Font font, String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            if (cp == '\t') { out.append("    "); return; }
            if (Character.isISOControl(cp)) return;
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                out.append('?');
            }
        });
        return out.toString();
    }
}
