package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.application.Requester;
import org.springframework.security.core.Authentication;

/** Converte o usuário autenticado (JWT) no {@link Requester} usado pelos casos de uso. */
final class RequesterResolver {

    private RequesterResolver() {}

    /** Sem autenticação (teste standalone) vale como staff; a SecurityConfig barra o acesso real. */
    static Requester from(Authentication authentication) {
        if (authentication == null) {
            return Requester.staff(null);
        }
        boolean client = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENT".equals(a.getAuthority()));
        return new Requester(authentication.getName(), client);
    }
}
