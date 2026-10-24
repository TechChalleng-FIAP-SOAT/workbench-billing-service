package com.workbench.workshop.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "akmsdkmasfnjsgknakmsdkmasdkmsadfkmdsakmdkmsadsa";
    private final JwtService service = new JwtService(new JwtProperties(SECRET));
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private String token(boolean client, long ttlMillis) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(client ? "52998224725" : "admin@workbench.dev")
                .claim("roles", client ? List.of("CLIENT") : List.of("ADMIN", "TECHNICIAN"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis));
        if (client) {
            builder.claim("clientId", 1L);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void parsesStaffTokenAndReadsRoles() {
        Claims claims = service.tryParseClaims(token(false, 60_000)).orElseThrow();
        assertFalse(service.isClientToken(claims));
        assertTrue(service.extractRoles(claims).contains("ADMIN"));
        assertFalse(service.isExpired(claims));
    }

    @Test
    void detectsClientToken() {
        Claims claims = service.tryParseClaims(token(true, 60_000)).orElseThrow();
        assertTrue(service.isClientToken(claims));
        assertEquals(List.of("CLIENT"), service.extractRoles(claims));
    }

    @Test
    void invalidSignatureReturnsEmpty() {
        assertTrue(service.tryParseClaims("not-a-jwt").isEmpty());
    }

    @Test
    void extractRolesEmptyWhenMissing() {
        String noRoles = Jwts.builder().subject("x")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key).compact();
        Claims claims = service.tryParseClaims(noRoles).orElseThrow();
        assertTrue(service.extractRoles(claims).isEmpty());
    }

    @Test
    void detectsExpiredToken() {
        // emite com expiração no passado e confirma isExpired (parse puro, sem validar exp)
        long now = System.currentTimeMillis();
        String expired = Jwts.builder().subject("x")
                .issuedAt(new Date(now - 10_000)).expiration(new Date(now - 5_000))
                .signWith(key).compact();
        // tryParseClaims rejeita expirado (lança) → vazio; então validamos isExpired direto num claims válido
        Claims valid = service.tryParseClaims(token(false, 60_000)).orElseThrow();
        assertFalse(service.isExpired(valid));
        assertTrue(service.tryParseClaims(expired).isEmpty());
    }
}
