package com.example.websecurity.waf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PathTraversalFilter extends OncePerRequestFilter {

    private static final String ALLOWED_FILE = "key1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String kidPath = extractKidFromToken(token);

            if (kidPath != null && !isPathSafeAndWhitelisted(kidPath)) {
                System.out.println("[DEBUG]: Malicious or unapproved Key ID path detected: " + kidPath);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "WAF: Malicious or unapproved Key ID path detected.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }


    private boolean isPathSafeAndWhitelisted(String fileName) {
        try {
            if (fileName.contains("..") || fileName.contains("%2e%2e")) {
                System.out.println("[DEBUG]: Path traversal attempt detected: " + fileName);
                return false;
            }

            Path normalizedPath = Paths.get(fileName).normalize();

            if (normalizedPath.isAbsolute()) {
                System.out.println("[DEBUG]: Absolute path detected: " + fileName);
                return false;
            }

            return fileName.equals(ALLOWED_FILE);

        } catch (Exception e) {
            System.out.println("[DEBUG]: Path validation failed: " + fileName);
            return false;
        }
    }


    private String extractKidFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

            Pattern pattern = Pattern.compile("\"kid\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(headerJson);

            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            System.out.println("[DEBUG]: Kid extraction failed");
        }
        return null;
    }
}