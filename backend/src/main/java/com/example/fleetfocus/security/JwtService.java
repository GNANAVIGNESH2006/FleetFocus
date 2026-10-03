package com.example.fleetfocus.security;

import com.example.fleetfocus.entity.SystemUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private Key signInKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured and non-empty");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey.trim());
        } catch (Exception ex) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded string", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes (256 bits)");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS must be a positive number");
        }
        this.signInKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof SystemUser systemUser && systemUser.getId() != null) {
            claims.put("uid", systemUser.getId());
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Number uid = claims.get("uid", Number.class);
            return uid != null ? uid.longValue() : null;
        });
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!Objects.equals(username, userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        if (userDetails instanceof SystemUser systemUser) {
            Long tokenUid = extractUserId(token);
            return tokenUid != null && Objects.equals(tokenUid, systemUser.getId());
        }
        return true;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        if (this.signInKey == null) {
            init();
        }
        return this.signInKey;
    }
}
