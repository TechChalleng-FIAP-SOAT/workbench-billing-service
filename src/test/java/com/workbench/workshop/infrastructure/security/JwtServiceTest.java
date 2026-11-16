package com.workbench.workshop.infrastructure.security;

import com.workbench.workshop.support.TestKeys;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService service = new JwtService(TestKeys.jwtProperties());
    private final PrivateKey key = TestKeys.authPrivateKey();

    private String token(boolean client, long ttlMillis) {
        long now = System.currentTimeMillis();
        var builder = TestKeys.authToken()
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
        String noRoles = TestKeys.authToken().subject("x")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key).compact();
        Claims claims = service.tryParseClaims(noRoles).orElseThrow();
        assertTrue(service.extractRoles(claims).isEmpty());
    }

    @Test
    void detectsExpiredToken() {
        // O parser já valida a expiração: token vencido não passa.
        long now = System.currentTimeMillis();
        String expired = TestKeys.authToken().subject("x")
                .issuedAt(new Date(now - 10_000)).expiration(new Date(now - 5_000))
                .signWith(key).compact();
        assertTrue(service.tryParseClaims(expired).isEmpty());
    }

    @Test
    void rejectsTokenSignedWithTheOldSharedSecret() {
        // HS256 com o antigo JWT_SECRET: quem tinha o segredo emitia token de ADMIN. Não vale mais.
        javax.crypto.SecretKey oldSecret = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "akmsdkmasfnjsgknakmsdkmasdkmsadfkmdsakmdkmsadsa".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String hs256 = io.jsonwebtoken.Jwts.builder().header().keyId(JwtService.AUTH_KEY_ID).and()
                .subject("admin@workbench.dev").claim("roles", List.of("ADMIN"))
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(oldSecret).compact();
        assertTrue(new JwtService(TestKeys.jwtProperties()).tryParseClaims(hs256).isEmpty());
    }

    @Test
    void rejectsUnknownKeyIdAndServiceTokenWhereNotTrusted() {
        JwtService onlyAuthKey = new JwtService(new JwtProperties(TestKeys.AUTH_PUBLIC, ""));
        String unknownKid = io.jsonwebtoken.Jwts.builder().header().keyId("outra-chave").and()
                .subject("x").expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(TestKeys.authPrivateKey()).compact();
        String serviceToken = io.jsonwebtoken.Jwts.builder().header().keyId(JwtService.SERVICE_KEY_ID).and()
                .subject("os-service").expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(TestKeys.privateKey(TestKeys.SERVICE_PRIVATE)).compact();

        assertTrue(onlyAuthKey.tryParseClaims(unknownKid).isEmpty());
        assertTrue(onlyAuthKey.tryParseClaims(serviceToken).isEmpty());
    }
}
