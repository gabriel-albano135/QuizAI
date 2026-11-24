package br.com.quizai.web;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = PdfController.class)
public class PdfUploadAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(413).body(
            Map.of("message", "O PDF deve ter no máximo 10 MB.")
        );
    }

    @ExceptionHandler(
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<Map<String, String>> badPage() {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Informe números inteiros para as páginas.")
        );
    }
}
