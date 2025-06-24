package org.sunbird.incredible.pojos;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.ob.CryptographicKey;

import static org.junit.jupiter.api.Assertions.*;

class SignatoryExtensionTest {

    @Test
    void testSignatoryExtensionGettersAndSetters() {
        String context = "https://example.org/context.json";
        SignatoryExtension signatory = new SignatoryExtension(context);

        // Test constructor sets context and type
        assertEquals(context, signatory.getContext());
        assertArrayEquals(new String[]{"Extension", "extensions:SignatoryExtension"}, signatory.getType());

        // Set and get designation
        String designation = "Director of Education";
        signatory.setDesignation(designation);
        assertEquals(designation, signatory.getDesignation());

        // Set and get image
        String image = "data:image/png;base64,iVBORw...";
        signatory.setImage(image);
        assertEquals(image, signatory.getImage());

        // Set and get name
        String name = "John Doe";
        signatory.setName(name);
        assertEquals(name, signatory.getName());

        // Set and get publicKey
        CryptographicKey key = new CryptographicKey("https://context.org");
        signatory.setPublicKey(key);
        assertEquals(key, signatory.getPublicKey());
    }

    @Test
    void testInheritedFieldsFromIdentityObject() {
        SignatoryExtension signatory = new SignatoryExtension("https://context");

        // Identity
        String identity = "did:example:123";
        signatory.setIdentity(identity);
        assertEquals(identity, signatory.getIdentity());

        // Type
        String[] types = {"SignatoryType"};
        signatory.setType(types);
        assertArrayEquals(types, signatory.getType());

        // Hashed
        signatory.setHashed(true);
        assertTrue(signatory.isHashed());

        // Salt
        String salt = "randomSalt";
        signatory.setSalt(salt);
        assertEquals(salt, signatory.getSalt());
    }
}
