package com.danasea.backend.modules.order.infrastructure.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.presentation.dtos.OrderReceiptResponse;

@Component
public class PdfReceiptGenerator {
    private static final float MARGIN = 45;
    private static final float FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 16;
    private static final int LINES_PER_PAGE = 43;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx");

    public byte[] generate(OrderReceiptResponse receipt) {
        try (PDDocument document = new PDDocument();
                InputStream regular = fontResource("NotoSans-Regular.ttf");
                InputStream bold = fontResource("NotoSans-Bold.ttf");
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(document, regular);
            PDType0Font boldFont = PDType0Font.load(document, bold);
            List<String> lines = new ArrayList<>();
            for (String line : receiptLines(receipt)) {
                lines.addAll(wrap(sanitize(line, font), font, PDRectangle.A4.getWidth() - 2 * MARGIN));
            }
            int pages = Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
            for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    draw(stream, boldFont, "DANASEA - ORDER RECEIPT", MARGIN, page.getMediaBox().getHeight() - MARGIN);
                    int from = pageIndex * LINES_PER_PAGE;
                    int to = Math.min(lines.size(), from + LINES_PER_PAGE);
                    float y = page.getMediaBox().getHeight() - MARGIN - 32;
                    for (String line : lines.subList(from, to)) {
                        draw(stream, line.startsWith("TOTAL AMOUNT:") ? boldFont : font, line, MARGIN, y);
                        y -= LINE_HEIGHT;
                    }
                    draw(stream, font, "Page " + (pageIndex + 1) + " / " + pages + " | " + receipt.receiptCode(), MARGIN, MARGIN);
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to generate PDF receipt.", failure);
        }
    }

    private List<String> receiptLines(OrderReceiptResponse receipt) {
        List<String> lines = new ArrayList<>();
        lines.add("Order ID: " + receipt.orderId());
        lines.add("Order Code: " + receipt.orderCode());
        lines.add("Receipt Code: " + receipt.receiptCode());
        lines.add("Customer ID: " + receipt.customerId());
        lines.add("Status: " + receipt.status());
        lines.add("Payment Method: " + (receipt.paymentProvider() == null ? "Not recorded" : receipt.paymentProvider()));
        lines.add("Paid At: " + (receipt.paidAt() == null ? "Not recorded" : receipt.paidAt().format(DATE_FORMAT)));
        lines.add("");
        lines.add("ITEMS & SERVICES");
        if (receipt.items() != null) {
            for (int i = 0; i < receipt.items().size(); i++) {
                var item = receipt.items().get(i);
                lines.add("#" + (i + 1) + ": " + (item.serviceName() == null ? "DANASEA service" : item.serviceName()));
                lines.add("Quantity: " + item.quantity() + " | Unit Price: " + money(item.unitPrice()));
                lines.add("Subtotal: " + money(item.subtotal()) + " | Discount: " + money(item.discountAmount())
                        + " | Final: " + money(item.finalAmount()));
                lines.add("");
            }
        }
        lines.add("Total Discount: " + money(receipt.discountAmount()));
        lines.add("TOTAL AMOUNT: " + money(receipt.totalAmount()));
        lines.add("");
        lines.add("Thank you for choosing DANASEA Marine Experience.");
        return lines;
    }

    private String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f VND", amount == null ? BigDecimal.ZERO : amount);
    }

    private InputStream fontResource(String name) throws IOException {
        InputStream resource = getClass().getResourceAsStream("/fonts/" + name);
        if (resource == null) {
            throw new IOException("Receipt font resource is missing: " + name);
        }
        return resource;
    }

    private String sanitize(String text, PDType0Font font) {
        String normalized = Normalizer.normalize(text.replaceAll("\\s+", " "), Normalizer.Form.NFC);
        StringBuilder safe = new StringBuilder();
        normalized.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            try {
                font.encode(character);
                safe.append(character);
            } catch (IOException | IllegalArgumentException unsupported) {
                safe.append('?');
            }
        });
        return safe.toString();
    }

    private List<String> wrap(String text, PDType0Font font, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && width(candidate, font) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            for (int codePoint : word.codePoints().toArray()) {
                String character = new String(Character.toChars(codePoint));
                if (!line.isEmpty() && width(line + character, font) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(character);
            }
        }
        lines.add(line.toString());
        return lines;
    }

    private float width(String text, PDType0Font font) throws IOException {
        return font.getStringWidth(text) / 1000 * FONT_SIZE;
    }

    private void draw(PDPageContentStream stream, PDType0Font font, String text, float x, float y) throws IOException {
        stream.beginText();
        stream.setFont(font, FONT_SIZE);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }
}
