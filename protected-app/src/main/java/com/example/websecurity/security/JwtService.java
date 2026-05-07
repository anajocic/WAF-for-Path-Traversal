package com.example.websecurity.security;

import com.example.websecurity.persistence.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Service
public class JwtService {

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("tokenType", "ACCESS");

        return Jwts.builder()
                .setHeaderParam("kid", "key1")
                .setClaims(extraClaims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .signWith(loadKeyFromFile("key1"))
                .compact();
    }

    private Key loadKeyFromFile(String kid) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get("keys/" + kid));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Key loading failed");
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public String extractExtraClaim(String claimName, String token) {
        return extractAllClaims(token).get(claimName, String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey(token))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractKid(String token) {
        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> header = mapper.readValue(headerJson, Map.class);

            return (String) header.get("kid");

        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT header");
        }
    }

    private Key getSignInKey(String token) {
        try {
            String kid = extractKid(token);

            String path = "keys/" + kid;
            System.out.println(path);
            byte[] keyBytes = Files.readAllBytes(Paths.get(path));

            return Keys.hmacShaKeyFor(keyBytes);

        } catch (Exception e) {
            throw new RuntimeException("Key loading failed");
        }
    }
}
