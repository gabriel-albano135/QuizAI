package br.com.quizai.service;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextService {

    public record ExtractedPdf(
        String text,
        int totalPages,
        int startPage,
        int endPage,
        List<Integer> emptyPages
    ) {}

    public ExtractedPdf extract(MultipartFile file, Integer start, Integer end) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException(
            "Selecione um PDF com conteúdo."
        );
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException(
            "O PDF deve ter no máximo 10 MB."
        );
        try {
            byte[] bytes = file.getBytes();
            if (
                bytes.length < 5 ||
                !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")
            ) throw new IllegalArgumentException("O arquivo enviado não é um PDF válido.");
            try (var document = Loader.loadPDF(bytes)) {
                if (
                    !document.getCurrentAccessPermission().canExtractContent()
                ) throw new IllegalArgumentException(
                    "Este PDF não permite extração de texto. Use uma cópia autorizada sem essa restrição."
                );
                int total = document.getNumberOfPages(),
                    first = start == null ? 1 : start,
                    last = end == null ? total : end;
                if (first < 1 || last < first || last > total) throw new IllegalArgumentException(
                    "Intervalo inválido. O PDF tem " +
                        total +
                        " páginas. Use os números das páginas do arquivo."
                );
                if (last - first + 1 > 50) throw new IllegalArgumentException(
                    "Selecione no máximo 50 páginas por importação. Este PDF tem " +
                        total +
                        " páginas."
                );
                var text = new BoundedWriter();
                var empty = new ArrayList<Integer>();
                var stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                for (int page = first; page <= last; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    int before = text.buffer.length();
                    stripper.writeText(document, text);
                    if (text.buffer.substring(before).isBlank()) empty.add(page);
                }
                String content = text.buffer.toString().strip();
                if (content.isBlank()) throw new IllegalArgumentException(
                    "Não foi encontrado texto extraível. O PDF pode ser digitalizado ou conter somente imagens. Use OCR antes de importar ou cole o texto manualmente."
                );
                return new ExtractedPdf(content, total, first, last, List.copyOf(empty));
            }
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException(
                "O PDF está protegido por senha. Envie uma cópia desbloqueada."
            );
        } catch (TextLimitException e) {
            throw new IllegalArgumentException(
                "O trecho ultrapassa 16.000 caracteres. Selecione menos páginas. O conteúdo anterior foi mantido; nenhum texto foi cortado."
            );
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Não foi possível ler o PDF. Confira se o arquivo está íntegro e tente outra cópia."
            );
        }
    }

    private static class TextLimitException extends IOException {}

    private static class BoundedWriter extends Writer {

        final StringBuilder buffer = new StringBuilder();

        public void write(char[] chars, int offset, int length) throws IOException {
            if (buffer.length() + length > 16000) throw new TextLimitException();
            buffer.append(chars, offset, length);
        }

        public void flush() {}

        public void close() {}
    }
}
