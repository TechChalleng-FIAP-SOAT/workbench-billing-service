package com.workbench.workshop.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class FakeProviderGuardTest {

    @Test
    void failsOutsideLocalProfileWithoutExplicitAllow() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new FakeProviderGuard(env, false));

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("mercadopago"));
    }

    @Test
    void failsWhenNoProfileIsActive() {
        assertThrows(IllegalStateException.class, () -> new FakeProviderGuard(new MockEnvironment(), false));
    }

    @Test
    void allowedOnLocalProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");

        assertDoesNotThrow(() -> new FakeProviderGuard(env, false));
    }

    @Test
    void allowedWhenExplicitlyEnabledForTests() {
        assertDoesNotThrow(() -> new FakeProviderGuard(new MockEnvironment(), true));
    }
}
