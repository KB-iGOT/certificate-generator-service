package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignedVerificationTest {

    @Test
    void testConstructorAndFields() {
        SignedVerification verification = new SignedVerification();

        // Verify default type is set correctly
        String[] expectedType = new String[]{"SignedBadge"};
        assertArrayEquals(expectedType, verification.getType());

        // Set and verify creator field
        String creator = "https://example.org/key/123";
        verification.setCreator(creator);
        assertEquals(creator, verification.getCreator());
    }
}
