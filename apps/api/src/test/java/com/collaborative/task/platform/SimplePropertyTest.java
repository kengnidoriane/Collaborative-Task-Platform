package com.collaborative.task.platform;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple property test to verify jqwik is working
 */
class SimplePropertyTest {

    @Test
    void simpleTest() {
        assertTrue(true, "Simple test should pass");
    }

    @Property(tries = 10)
    void stringIsNotEmpty(@ForAll @NotBlank String text) {
        assertFalse(text.isEmpty(), "Non-blank string should not be empty");
        assertFalse(text.trim().isEmpty(), "Non-blank string should not be whitespace only");
    }
}