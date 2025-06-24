package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {

    @Test
    void testAllGettersAndSetters() {
        Profile profile = new Profile("https://example.org/context");

        String id = "https://example.org/profile/123";
        String[] type = new String[]{"ProfileType"};
        String name = "Sample Training Institute";
        String email = "contact@example.org";
        String url = "https://example.org";
        String description = "This is a test profile.";
        String[] publicKey = new String[]{"https://example.org/key.pem"};
        String[] revocationList = new String[]{"https://example.org/revoked1"};
        String telephone = "+91-1234567890";
        VerificationObject verification = new VerificationObject();

        profile.setId(id);
        profile.setType(type);
        profile.setName(name);
        profile.setEmail(email);
        profile.setUrl(url);
        profile.setDescription(description);
        profile.setPublicKey(publicKey);
        profile.setRevocationList(revocationList);
        profile.setTelephone(telephone);
        profile.setVerification(verification);

        // Assertions
        assertEquals(id, profile.getId());
        assertArrayEquals(type, profile.getType());
        assertEquals(name, profile.getName());
        assertEquals(email, profile.getEmail());
        assertEquals(url, profile.getUrl());
        assertEquals(description, profile.getDescription());
        assertArrayEquals(publicKey, profile.getPublicKey());
        assertArrayEquals(revocationList, profile.getRevocationList());
        assertEquals(telephone, profile.getTelephone());
        assertEquals(verification, profile.getVerification());
        assertEquals("https://example.org/context", profile.getContext());
    }
}
