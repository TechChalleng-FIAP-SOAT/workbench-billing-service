package com.workbench.workshop.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Secret HS256 compartilhado por toda a plataforma (mesmo JWT_SECRET da Lambda/os-service). */
@Configuration
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JwtProperties {
    @Value("${jwt.secret}") private String secret;
}
