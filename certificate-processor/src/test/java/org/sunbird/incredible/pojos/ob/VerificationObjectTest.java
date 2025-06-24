package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VerificationObjectTest {

    @Test
    void testGettersAndSetters() {
        VerificationObject verificationObject = new VerificationObject();

        // Test type field
        String[] type = {"HostedBadge", "SignedBadge"};
        verificationObject.setType(type);
        assertArrayEquals(type, verificationObject.getType());

        // Test verificationProperty
        String verificationProperty = "id";
        verificationObject.setVerificationProperty(verificationProperty);
        assertEquals(verificationProperty, verificationObject.getVerificationProperty());

        // Test startsWith
        String startsWith = "https://example.org/";
        verificationObject.setStartsWith(startsWith);
        assertEquals(startsWith, verificationObject.getStartsWith());

        // Test allowedOrigins
        List<String> origins = Arrays.asList("example.org", "another.example.org");
        verificationObject.setAllowedOrigins(origins);
        assertEquals(origins, verificationObject.getAllowedOrigins());
    }
}
