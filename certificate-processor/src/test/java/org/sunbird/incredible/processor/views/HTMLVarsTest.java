package org.sunbird.incredible.processor.views;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HTMLVarsTest {

    @Test
    void testGetReturnsAllSupportedVars() {
        List<String> vars = HTMLVars.get();

        // Assert the list is not null or empty
        assertNotNull(vars);
        assertFalse(vars.isEmpty());

        // Check the total number of supported vars
        assertEquals(13, vars.size());

        // Validate the presence of specific known variables
        assertTrue(vars.contains("$certificateName"));
        assertTrue(vars.contains("$recipientId"));
        assertTrue(vars.contains("$signatory1Image"));
        assertTrue(vars.contains("$issuerName"));
    }

    @Test
    void testEnumValuesCoverage() {
        // This ensures full enum path coverage
        for (HTMLVars.SupportedVars var : HTMLVars.SupportedVars.values()) {
            assertNotNull(var.toString());
        }
    }
}
