package com.workbench.workshop.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtFilterMaskTest {

    @Test
    void masksCpf() {
        assertEquals("529******25", JwtFilter.mask("52998224725"));
    }

    @Test
    void keepsEmailAndShortValues() {
        assertEquals("admin@workbench.dev", JwtFilter.mask("admin@workbench.dev"));
        assertEquals("abc", JwtFilter.mask("abc"));
        assertNull(JwtFilter.mask(null));
    }
}
