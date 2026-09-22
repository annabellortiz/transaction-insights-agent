package com.txi.api;

import com.txi.service.ingest.CsvIngestionResult;
import com.txi.service.ingest.CsvIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final CsvIngestionService ingestionService;

    @Autowired
    public UploadController(CsvIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<CsvIngestionResult> uploadCsv(@RequestPart("file") MultipartFile file) {
        CsvIngestionResult result = ingestionService.ingest(file);
        return ResponseEntity.ok(result);
    }
}
