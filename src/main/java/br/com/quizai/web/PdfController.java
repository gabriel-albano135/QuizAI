package br.com.quizai.web;

import br.com.quizai.service.PdfTextService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfTextService service;

    public PdfController(PdfTextService service) {
        this.service = service;
    }

    @PostMapping(value = "/extrair", consumes = "multipart/form-data")
    public PdfTextService.ExtractedPdf extract(
        @RequestParam MultipartFile file,
        @RequestParam(required = false) Integer startPage,
        @RequestParam(required = false) Integer endPage
    ) {
        return service.extract(file, startPage, endPage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
