package org.sunbird.auth;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyManagerTest {

    @BeforeEach
    void clearKeyMap() throws Exception {
        // Reset static keyMap before test
        Field keyMapField = KeyManager.class.getDeclaredField("keyMap");
        keyMapField.setAccessible(true);
        ((Map<?, ?>) keyMapField.get(null)).clear();
    }

    @Test
    void getPublicKeyReturnsNullIfKeyNotPresent() {
        assertNull(KeyManager.getPublicKey("nonexistent"));
    }

    @Test
    void loadPublicKeyReturnsPublicKeyOnValidInput() throws Exception {
        String pem = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn\n-----END PUBLIC KEY-----";
        byte[] fakeKey = new byte[0];
        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class);
             MockedStatic<KeyFactory> kfMock = mockStatic(KeyFactory.class)) {
            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
                    .thenReturn(fakeKey);
            KeyFactory kf = mock(KeyFactory.class);
            kfMock.when(() -> KeyFactory.getInstance("RSA")).thenReturn(kf);
            PublicKey pk = mock(PublicKey.class);
            when(kf.generatePublic(any(X509EncodedKeySpec.class))).thenReturn(pk);

            PublicKey result = KeyManager.loadPublicKey(pem);
            assertEquals(pk, result);
        }
    }

    @Test
    void loadPublicKeyThrowsExceptionOnInvalidKey() {
        String pem = "-----BEGIN PUBLIC KEY-----\nINVALIDKEY\n-----END PUBLIC KEY-----";
        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
                    .thenThrow(new IllegalArgumentException("bad base64"));
            assertThrows(Exception.class, () -> KeyManager.loadPublicKey(pem));
        }
    }

    @Test
    void initShouldLoadPublicKeysFromDirectory() throws Exception {
        Path mockPath = Paths.get("test-key.pub");
        Stream<Path> mockStream = Stream.of(mockPath);
        PublicKey mockPublicKey = mock(PublicKey.class);
        String dummyKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBg...\n-----END PUBLIC KEY-----";

        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<KeyManager> keyManagerMock = mockStatic(KeyManager.class, Mockito.CALLS_REAL_METHODS)
        ) {
            // ✅ Inject mock PropertiesCache via reflection before calling init
            PropertiesCache mockCache = mock(PropertiesCache.class);
            when(mockCache.getProperty(JsonKeys.ACCESS_TOKEN_PUBLICKEY_BASEPATH)).thenReturn("/mock/path");

            Field cacheField = KeyManager.class.getDeclaredField("propertiesCache");
            cacheField.setAccessible(true);
            cacheField.set(null, mockCache);

            // Mock Files
            filesMock.when(() -> Files.walk(Paths.get("/mock/path"))).thenReturn(mockStream);
            filesMock.when(() -> Files.isRegularFile(mockPath)).thenReturn(true);
            filesMock.when(() -> Files.lines(mockPath, StandardCharsets.UTF_8))
                    .thenReturn(Arrays.stream(dummyKey.split("\n")));

            // Mock loadPublicKey
            keyManagerMock.when(() -> KeyManager.loadPublicKey(any())).thenReturn(mockPublicKey);

            // Execute init
            KeyManager.init();

            // Validate map is populated
            Field keyMapField = KeyManager.class.getDeclaredField("keyMap");
            keyMapField.setAccessible(true);
            Map<String, KeyData> keyMap = (Map<String, KeyData>) keyMapField.get(null);

            assertFalse(keyMap.isEmpty());
            assertEquals(mockPublicKey, keyMap.get("test-key.pub").getPublicKey());
        }
    }
}

