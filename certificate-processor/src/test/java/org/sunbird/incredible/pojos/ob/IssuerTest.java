package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IssuerTest {

    @Test
    void testIssuerGettersAndSetters() {
        String ctx = "https://example.org/context";
        Issuer issuer = new Issuer(ctx);

        // Set values
        String id = "issuer-id";
        String[] type = {"Issuer", "Organization"};
        String context = "https://new.context.org";
        String name = "Awarding Body";
        String email = "contact@awards.org";
        String url = "https://awards.org";
        String[] publicKey = {"https://key1.pem", "https://key2.pem"};

        issuer.setId(id);
        issuer.setType(type);
        issuer.setContext(context);
        issuer.setName(name);
        issuer.setEmail(email);
        issuer.setUrl(url);
        issuer.setPublicKey(publicKey);

        // Assert values
        assertEquals(id, issuer.getId());
        assertArrayEquals(type, issuer.getType());
        assertEquals(context, issuer.getContext());
        assertEquals(name, issuer.getName());
        assertEquals(email, issuer.getEmail());
        assertEquals(url, issuer.getUrl());
        assertArrayEquals(publicKey, issuer.getPublicKey());
    }

    @Test
    void testIssuerConstructorInitializesContextAndType() {
        String ctx = "https://issuer.context";
        Issuer issuer = new Issuer(ctx);

        assertEquals(ctx, issuer.getContext());
        assertArrayEquals(new String[]{"Issuer"}, issuer.getType());
    }
}
