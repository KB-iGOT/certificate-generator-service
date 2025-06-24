package org.sunbird.cert.actor;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HTMLValidatorResponseTest {

    @Test
    void validFlagIsSetAndRetrievedCorrectly() {
        HTMLValidatorResponse response = new HTMLValidatorResponse();
        response.setValid(Boolean.TRUE);
        assertTrue(response.getValid());
    }

    @Test
    void messageIsSetWithInvalidVarsAndRetrievedCorrectly() {
        Set<String> invalidVars = Set.of("var1", "var2");
        HTMLValidatorResponse response = new HTMLValidatorResponse();
        response.setMessage(invalidVars);
        assertNotNull(response.getMessage());
        assertEquals(invalidVars, response.getMessage().getInvalidVars());
    }

    @Test
    void messageIsNullWhenNotSet() {
        HTMLValidatorResponse response = new HTMLValidatorResponse();
        assertNull(response.getMessage());
    }

    @Test
    void validIsNullByDefault() {
        HTMLValidatorResponse response = new HTMLValidatorResponse();
        assertNull(response.getValid());
    }
}