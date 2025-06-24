package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptographicKeyTest {

    private CryptographicKey key;

    @BeforeEach
    void setUp() {
        key = new CryptographicKey("https://example.org/context");
    }

    @Test
    void testConstructorAndContext() {
        assertEquals("https://example.org/context", key.getContext());
    }

    @Test
    void testDefaultType() {
        assertArrayEquals(new String[]{"CryptographicKey"}, key.getType());
    }

    @Test
    void testSetAndGetType() {
        String[] newType = {"Key", "CryptographicKey"};
        key.setType(newType);
        assertArrayEquals(newType, key.getType());
    }

    @Test
    void testSetAndGetId() {
        key.setId("https://example.org/key/123");
        assertEquals("https://example.org/key/123", key.getId());
    }

    @Test
    void testSetAndGetOwner() {
        key.setOwner("https://example.org/issuer/abc");
        assertEquals("https://example.org/issuer/abc", key.getOwner());
    }

    @Test
    void testSetAndGetPublicKeyPem() {
        String pem = "-----BEGIN PUBLIC KEY-----\nABC123XYZ==\n-----END PUBLIC KEY-----";
        key.setPublicKeyPem(pem);
        assertEquals(pem, key.getPublicKeyPem());
    }

    @Test
    void testSetAndGetVersion() {
        key.setVersion("1.0");
        assertEquals("1.0", key.getVersion());
    }
}
