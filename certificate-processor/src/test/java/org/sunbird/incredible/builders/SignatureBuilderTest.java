package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.Signature;

import static org.junit.jupiter.api.Assertions.*;

class SignatureBuilderTest {

    @Test
    void testSignatureBuilder_allFields() {
        SignatureBuilder builder = new SignatureBuilder();
        Signature signature = builder
                .setType("RsaSignature2018")
                .setCreator("https://example.org/keys/1")
                .setCreated("2025-06-19T10:00:00Z")
                .setSignatureValue("Base64EncodedSignature==")
                .build();

        assertNotNull(signature);
        assertEquals("RsaSignature2018", signature.getType());
        assertEquals("https://example.org/keys/1", signature.getCreator());
        assertEquals("2025-06-19T10:00:00Z", signature.getCreated());
        assertEquals("Base64EncodedSignature==", signature.getSignatureValue());
    }
}
