package br.com.quizai.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class PdfTextServiceTest {

    private final PdfTextService service = new PdfTextService();

    public static byte[] pdf(String... pages) throws IOException {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            for (String text : pages) {
                var page = new PDPage();
                doc.addPage(page);
                if (!text.isEmpty()) try (var stream = new PDPageContentStream(doc, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    public static MockMultipartFile upload(byte[] bytes) {
        return new MockMultipartFile("file", "aula.pdf", "application/pdf", bytes);
    }

    @Test
    void extractsOnlySelectedPages() throws Exception {
        var result = service.extract(
            upload(pdf("Primeira pagina", "Memoria RAM e volatil.", "Terceira pagina")),
            2,
            2
        );
        assertEquals("Memoria RAM e volatil.", result.text());
        assertEquals(3, result.totalPages());
        assertEquals(2, result.startPage());
    }

    @Test
    void mixedPdfReportsEmptyPages() throws Exception {
        var result = service.extract(upload(pdf("Texto selecionavel", "")), null, null);
        assertEquals(java.util.List.of(2), result.emptyPages());
    }

    @Test
    void imageOnlyOrBlankPdfExplainsOcr() throws Exception {
        var f = upload(pdf(""));
        assertTrue(
            assertThrows(IllegalArgumentException.class, () -> service.extract(f, null, null))
                .getMessage()
                .contains("OCR")
        );
    }

    @Test
    void rejectsInvalidEmptyAndOversizedFiles() {
        assertThrows(IllegalArgumentException.class, () ->
            service.extract(upload(new byte[0]), null, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.extract(upload("not a pdf".getBytes()), null, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.extract(upload(new byte[10 * 1024 * 1024 + 1]), null, null)
        );
    }

    @Test
    void rejectsBadRangesAndTooManyPages() throws Exception {
        var f = upload(pdf("um", "dois"));
        assertThrows(IllegalArgumentException.class, () -> service.extract(f, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> service.extract(f, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> service.extract(f, 1, 3));
        var many = new String[51];
        java.util.Arrays.fill(many, "Texto");
        var large = upload(pdf(many));
        assertThrows(IllegalArgumentException.class, () -> service.extract(large, null, null));
        assertEquals(1, service.extract(large, 1, 1).endPage());
    }

    @Test
    void refusesExcessTextWithoutTruncating() throws Exception {
        var f = upload(pdf("texto ".repeat(3000)));
        assertTrue(
            assertThrows(IllegalArgumentException.class, () -> service.extract(f, null, null))
                .getMessage()
                .contains("16.000")
        );
    }

    @Test
    void rejectsPasswordProtectedPdf() throws Exception {
        byte[] bytes;
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            var policy = new StandardProtectionPolicy(
                "owner-test",
                "reader-test",
                new AccessPermission()
            );
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(out);
            bytes = out.toByteArray();
        }
        var f = upload(bytes);
        assertTrue(
            assertThrows(IllegalArgumentException.class, () -> service.extract(f, null, null))
                .getMessage()
                .contains("senha")
        );
    }
}
