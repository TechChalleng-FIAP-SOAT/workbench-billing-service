package com.workbench.workshop.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.CrossOriginEmbedderPolicyHeaderWriter.CrossOriginEmbedderPolicy;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy;

/**
 * Segurança do Billing Service (estratégia de defesa em profundidade: cada serviço
 * valida o JWT, mesmo nas chamadas internas). Stateless, sem sessão, API REST.
 *
 * <p>Rotas públicas (sem token):
 * <ul>
 *   <li>health/actuator e Swagger/OpenAPI;</li>
 *   <li>{@code POST /api/payments/webhook} — webhook SIMULADO, que só existe com
 *       {@code payment.provider=fake} (local). Com o Mercado Pago real o endpoint nem é
 *       registrado (404) e a confirmação vem do job de consulta.</li>
 * </ul>
 * Demais rotas exigem token. Consulta de orçamento/pagamento é liberada ao cliente
 * dono (ROLE_CLIENT) e ao staff; operações de escrita/estorno ficam com o staff.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JsonSecurityErrorHandler errorHandler;

    public SecurityConfig(JwtFilter jwtFilter, JsonSecurityErrorHandler errorHandler) {
        this.jwtFilter = jwtFilter;
        this.errorHandler = errorHandler;
    }

    // S4502 (CSRF): seguro desabilitar — API REST stateless com JWT (sem cookie de sessão),
    // logo não há vetor de CSRF. Suprimido com justificativa em vez de review manual no Sonar.
    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .frameOptions(fo -> fo.deny())                 // anti-clickjacking (OWASP A05)
                        .contentTypeOptions(c -> {})                  // X-Content-Type-Options: nosniff
                        // Isolamento cross-origin (ZAP 90004 / Spectre): a API não é embutida por outros sites.
                        .crossOriginResourcePolicy(c -> c.policy(CrossOriginResourcePolicy.SAME_ORIGIN))
                        .crossOriginOpenerPolicy(c -> c.policy(CrossOriginOpenerPolicy.SAME_ORIGIN))
                        .crossOriginEmbedderPolicy(c -> c.policy(CrossOriginEmbedderPolicy.REQUIRE_CORP)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401/403 em JSON (mesmo formato dos demais erros), não resposta vazia.
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Webhook simulado (só existe com payment.provider=fake).
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        // Consultas de orçamento e pagamento: cliente dono + staff.
                        .requestMatchers(HttpMethod.GET, "/api/budgets/**", "/api/payments/**")
                            .hasAnyRole("CLIENT", "USER", "ADMIN", "TECHNICIAN")
                        .anyRequest().hasAnyRole("ADMIN", "TECHNICIAN"))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
