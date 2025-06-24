package org.sunbird.auth;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.util.Time;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidatorTest {

    @BeforeEach
    void resetPropertiesCache() {
        try {
            var field = AccessTokenValidator.class.getDeclaredField("propertiesCache");
            field.setAccessible(true);
            field.set(null, mock(PropertiesCache.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void verifyUserTokenReturnsUnauthorizedOnException() {
        String invalidToken = "invalid.token";
        String result = AccessTokenValidator.verifyUserToken(invalidToken, true);
        assertEquals(JsonKeys.UNAUTHORIZED, result);
    }

    @Test
    void verifyUserTokenReturnsUnauthorizedIfSubIsBlank() {
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"RS256\",\"kid\":\"test-key\"}".getBytes());
        String body = Base64.getEncoder().encodeToString("{\"iss\":\"https://sso.example.com/realms/sunbird\",\"sub\":\" \"}".getBytes());
        String token = header + "." + body + ".signature";

        // This is the correct mock for the return type of KeyManager.getPublicKey()
        KeyData mockKeyData = mock(KeyData.class);

        try (
                MockedStatic<PropertiesCache> cacheMock = mockStatic(PropertiesCache.class);
                MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
                MockedStatic<KeyManager> keyManagerMock = mockStatic(KeyManager.class)
        ) {
            PropertiesCache mockCache = mock(PropertiesCache.class);
            cacheMock.when(PropertiesCache::getInstance).thenReturn(mockCache);

            cryptoMock.when(() -> CryptoUtil.verifyRSASign(any(), any(), any(), any())).thenReturn(true);
            keyManagerMock.when(() -> KeyManager.getPublicKey(any())).thenReturn(mockKeyData);

            String result = AccessTokenValidator.verifyUserToken(token, true);
            assertEquals(JsonKeys.UNAUTHORIZED, result);
        }
    }


    @Test
    void verifyManagedUserTokenReturnsUnauthorizedOnException() {
        String invalidToken = "invalid.token";
        String result = AccessTokenValidator.verifyManagedUserToken(invalidToken, "by", "for", "headers");
        assertEquals(JsonKeys.UNAUTHORIZED, result);
    }

    @Test
    void verifyManagedUserTokenReturnsUnauthorizedIfParentIdMismatch() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put(JsonKeys.PARENT_ID, "parent");
        payload.put(JsonKeys.SUB, "managed");
        try (MockedStatic<AccessTokenValidator> validatorMock = Mockito.mockStatic(AccessTokenValidator.class, Mockito.CALLS_REAL_METHODS)) {
            String result = AccessTokenValidator.verifyManagedUserToken("header.body.signature", "other", "managed", "headers");
            assertEquals(JsonKeys.UNAUTHORIZED, result);
        }
    }

    @Test
    void checkIssReturnsTrueForMatchingRealm() throws Exception {
        PropertiesCache cache = mock(PropertiesCache.class);
        when(cache.getProperty(JsonKeys.SUNBIRD_SSO_URL)).thenReturn("https://sso.example.com/");
        when(cache.getProperty(JsonKeys.SUNBIRD_SSO_RELAM)).thenReturn("sunbird");
        var field = AccessTokenValidator.class.getDeclaredField("propertiesCache");
        field.setAccessible(true);
        field.set(null, cache);
        Method m = AccessTokenValidator.class.getDeclaredMethod("checkIss", String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, "https://sso.example.com/realms/sunbird");
        assertTrue(result);
    }

    @Test
    void checkIssReturnsFalseForNonMatchingRealm() throws Exception {
        PropertiesCache cache = mock(PropertiesCache.class);
        when(cache.getProperty(JsonKeys.SUNBIRD_SSO_URL)).thenReturn("https://sso.example.com/");
        when(cache.getProperty(JsonKeys.SUNBIRD_SSO_RELAM)).thenReturn("sunbird");
        var field = AccessTokenValidator.class.getDeclaredField("propertiesCache");
        field.setAccessible(true);
        field.set(null, cache);
        Method m = AccessTokenValidator.class.getDeclaredMethod("checkIss", String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, "https://other.com/realms/sunbird");
        assertFalse(result);
    }

    @Test
    void isExpiredReturnsTrueIfCurrentTimeGreaterThanExpiration() throws Exception {
        try (MockedStatic<Time> timeMock = mockStatic(Time.class)) {
            timeMock.when(Time::currentTime).thenReturn(2000); // Use int, not long
            Method m = AccessTokenValidator.class.getDeclaredMethod("isExpired", Integer.class);
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(null, 1000);
            assertTrue(result);
        }
    }


    @Test
    void isExpiredReturnsFalseIfCurrentTimeLessThanExpiration() throws Exception {
        try (MockedStatic<Time> timeMock = mockStatic(Time.class)) {
            timeMock.when(Time::currentTime).thenReturn(1000);
            Method m = AccessTokenValidator.class.getDeclaredMethod("isExpired", Integer.class);
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(null, 2000);
            assertFalse(result);
        }
    }


    @Test
    void decodeFromBase64ReturnsDecodedBytes() throws Exception {
        String data = Base64.getEncoder().encodeToString("hello".getBytes());
        Method m = AccessTokenValidator.class.getDeclaredMethod("decodeFromBase64", String.class);
        m.setAccessible(true);
        byte[] result = (byte[]) m.invoke(null, data);
        assertEquals("hello", new String(result));
    }

    @Test
    void decodeFromBase64ReturnsEmptyForEmptyString() throws Exception {
        Method m = AccessTokenValidator.class.getDeclaredMethod("decodeFromBase64", String.class);
        m.setAccessible(true);
        byte[] result = (byte[]) m.invoke(null, "");
        assertEquals(0, result.length);
    }

}