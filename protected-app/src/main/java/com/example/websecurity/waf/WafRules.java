package com.example.websecurity.waf;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public class WafRules {

    public static final Pattern white_list = Pattern.compile("^[a-zA-Z0-9-]{1,64}$");

    public static final List<Pattern> black_list = List.of(
            Pattern.compile("\\.\\."),
            Pattern.compile("%2e%2e", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%252e", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%c0%ae", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%ef%bc%8f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\x2e\\x2e"),
            Pattern.compile("/"),
            Pattern.compile("\\\\")
    );

    public static boolean isValidPath(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            String decoded = java.net.URLDecoder.decode(path, "UTF-8");
            for (Pattern p : black_list) {
                if (p.matcher(decoded).find() || p.matcher(path).find()) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        for (Pattern p : black_list) {
            if (p.matcher(filename).find()) {
                return false;
            }
        }

        Pattern filenameWhitelist = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");
        if (!filenameWhitelist.matcher(filename).matches()) {
            return false;
        }

        try {
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            Path resolved = uploadDir.resolve(filename).normalize();

            if (!resolved.startsWith(uploadDir)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
