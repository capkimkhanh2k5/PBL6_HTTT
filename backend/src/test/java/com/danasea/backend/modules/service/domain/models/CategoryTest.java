package com.danasea.backend.modules.service.domain.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTest {

    @Test
    @DisplayName("isActive should return true only when isActive is Boolean.TRUE")
    void shouldEvaluateIsActiveCorrectly() {
        Category active = Category.builder().isActive(true).build();
        assertTrue(active.isActive());

        Category inactive = Category.builder().isActive(false).build();
        assertFalse(inactive.isActive());

        Category nullActive = Category.builder().isActive(null).build();
        assertFalse(nullActive.isActive());
    }
}
