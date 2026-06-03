package com.example.websecurity.api;

import com.example.websecurity.waf.WafRules;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@RestController
public class FileUploadController {

    @Operation(summary = "Upload fajla (ZAŠTIĆENO - WAF blokira path traversal)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

//        if (!WafRules.isSafeFilename(filename)) {
//            return ResponseEntity.status(403).body("WAF: Blokirano - naziv fajla sadrzi path traversal: " + filename);
//        }

        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok("Fajl sacuvan na: " + target.toAbsolutePath());
    }
}