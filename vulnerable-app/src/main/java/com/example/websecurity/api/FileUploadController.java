package com.example.websecurity.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
public class FileUploadController {

    @Operation(summary = "Upload fajla (RANJIVO - path traversal)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        String filename = file.getOriginalFilename();

        Path uploadDir = Paths.get("uploads");
        Path target = uploadDir.resolve(filename);

        Files.createDirectories(target.getParent() != null ? target.getParent() : uploadDir);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok("Fajl sacuvan na: " + target.toAbsolutePath());
    }
}
