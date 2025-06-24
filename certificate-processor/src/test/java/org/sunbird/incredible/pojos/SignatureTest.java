package org.sunbird.incredible.pojos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignatureTest {

    @Test
    void testGettersAndSetters() {
        Signature signature = new Signature();

        // Default type
        assertEquals("LinkedDataSignature2015", signature.getType());

        // Set and get type
        String type = "NewSignatureType";
        signature.setType(type);
        assertEquals(type, signature.getType());

        // Set and get creator
        String creator = "https://example.org/key#123";
        signature.setCreator(creator);
        assertEquals(creator, signature.getCreator());

        // Set and get created timestamp
        String created = "2024-12-01T12:34:56Z";
        signature.setCreated(created);
        assertEquals(created, signature.getCreated());

        // Set and get signature value
        String signatureValue = "abcd1234xyz=";
        signature.setSignatureValue(signatureValue);
        assertEquals(signatureValue, signature.getSignatureValue());
    }
}
