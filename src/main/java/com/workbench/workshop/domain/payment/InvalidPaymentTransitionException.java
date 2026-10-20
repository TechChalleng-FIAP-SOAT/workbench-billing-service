package com.workbench.workshop.domain.payment;

public class InvalidPaymentTransitionException extends RuntimeException {
    public InvalidPaymentTransitionException(String message) {
        super(message);
    }
}
