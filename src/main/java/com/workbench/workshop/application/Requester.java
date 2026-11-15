package com.workbench.workshop.application;

/**
 * Quem fez a requisição: o {@code subject} do JWT (CPF/CNPJ para cliente, e-mail para
 * staff) e se é um token de cliente ({@code ROLE_CLIENT}). Cliente só acessa o
 * orçamento/pagamento do próprio documento; staff acessa todos.
 */
public record Requester(String subject, boolean client) {

    public static Requester staff(String subject) {
        return new Requester(subject, false);
    }

    /** True quando pode acessar dados do cliente dono deste documento (nulo = só staff). */
    public boolean canAccess(String customerDocument) {
        return !client || (subject != null && customerDocument != null
                && digits(subject).equals(digits(customerDocument)));
    }

    public void requireAccessTo(String customerDocument) {
        if (!canAccess(customerDocument)) {
            throw new SecurityException("Cliente não autorizado a acessar dados de outro cliente.");
        }
    }

    private static String digits(String document) {
        return document.replaceAll("[^0-9]", "");
    }
}
