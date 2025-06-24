package org.sunbird.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;

import static org.mockito.Mockito.mock;

class KeyDataTest {

    @Test
    void constructorInitializesFieldsCorrectly() {
        PublicKey publicKey = mock(PublicKey.class);
        KeyData keyData = new KeyData("kid1", publicKey);
        Assertions.assertEquals("kid1", keyData.getKeyId());
        Assertions.assertEquals(publicKey, keyData.getPublicKey());
    }

    @Test
    void setKeyIdUpdatesKeyId() {
        KeyData keyData = new KeyData("oldId", mock(PublicKey.class));
        keyData.setKeyId("newId");
        Assertions.assertEquals("newId", keyData.getKeyId());
    }

    @Test
    void setPublicKeyUpdatesPublicKey() {
        PublicKey oldKey = mock(PublicKey.class);
        PublicKey newKey = mock(PublicKey.class);
        KeyData keyData = new KeyData("kid", oldKey);
        keyData.setPublicKey(newKey);
        Assertions.assertEquals(newKey, keyData.getPublicKey());
    }
}