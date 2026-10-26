package com.workbench.workshop.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica requisições a partir do JWT verificado (mesmo padrão do OS Service).
 * Tokens de cliente (claim clientId) recebem ROLE_CLIENT; tokens de staff recebem
 * as roles das claims (ADMIN/TECHNICIAN).
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    public static final String USER = "user";

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = authHeader.substring(7);
                jwtService.tryParseClaims(token)
                        .filter(claims -> !jwtService.isExpired(claims))
                        .ifPresent(this::authenticate);
            }
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                MDC.put(USER, mask(authentication.getName()));
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(USER);
        }
    }

    /** Mascara o subject (CPF do cliente) antes do MDC/log — OWASP A09. E-mails passam. */
    static String mask(String subject) {
        if (subject == null || subject.contains("@") || subject.length() < 6) {
            return subject;
        }
        return subject.substring(0, 3) + "*".repeat(subject.length() - 5)
                + subject.substring(subject.length() - 2);
    }

    private void authenticate(Claims claims) {
        List<SimpleGrantedAuthority> authorities;
        if (jwtService.isClientToken(claims)) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
        } else {
            authorities = jwtService.extractRoles(claims).stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
