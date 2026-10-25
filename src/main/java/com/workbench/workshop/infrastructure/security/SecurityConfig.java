package com.workbench.workshop.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Segurança do Billing Service (estratégia de defesa em profundidade: cada serviço
 * valida o JWT, mesmo nas chamadas internas). Stateless, sem sessão, API REST.
 *
 * <p>Rotas públicas (sem token):
 * <ul>
 *   <li>health/actuator e Swagger/OpenAPI;</li>
 *   <li>{@code POST /api/payments/webhook} — chamado pelo gateway de pagamento externo
 *       (ex.: Mercado Pago), que não envia o nosso JWT.</li>
 * </ul>
 * Demais rotas exigem token. Consulta de orçamento/pagamento é liberada ao cliente
 * dono (ROLE_CLIENT) e ao staff; operações de escrita/estorno ficam com o staff.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) { this.jwtFilter = jwtFilter; }

    // S4502 (CSRF): seguro desabilitar — API REST stateless com JWT (sem cookie de sessão),
    // logo não há vetor de CSRF. Suprimido com justificativa em vez de review manual no Sonar.
    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .frameOptions(fo -> fo.deny())
                        .contentTypeOptions(c -> {}))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Webhook do gateway de pagamento externo: sem JWT nosso.
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        // Consultas de orçamento e pagamento: cliente dono + staff.
                        .requestMatchers(HttpMethod.GET, "/api/budgets/**", "/api/payments/**")
                            .hasAnyRole("CLIENT", "USER", "ADMIN", "TECHNICIAN")
                        .anyRequest().hasAnyRole("ADMIN", "TECHNICIAN"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exAuth) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exAuth.getMessage())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
