package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.ob.Issuer;

import static org.junit.jupiter.api.Assertions.*;

class IssuerBuilderTest {

    @Test
    void testIssuerBuilder_allFields() {
        String[] types = new String[]{"Organization"};
        String[] publicKeys = new String[]{"key1", "key2"};

        IssuerBuilder builder = new IssuerBuilder("https://schema.org");
        Issuer issuer = builder
                .setId("did:example:issuer123")
                .setType(types)
                .setName("Test Issuer")
                .setEmail("issuer@example.org")
                .setUrl("https://issuer.example.org")
                .setPublicKey(publicKeys)
                .build();

        assertNotNull(issuer);
        assertEquals("did:example:issuer123", issuer.getId());
        assertArrayEquals(types, issuer.getType());
        assertEquals("Test Issuer", issuer.getName());
        assertEquals("issuer@example.org", issuer.getEmail());
        assertEquals("https://issuer.example.org", issuer.getUrl());
        assertArrayEquals(publicKeys, issuer.getPublicKey());
        assertEquals("https://schema.org", issuer.getContext());
    }
}
