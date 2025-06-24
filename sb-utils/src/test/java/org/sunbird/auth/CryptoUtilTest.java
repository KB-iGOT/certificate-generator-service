package org.sunbird.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.*;

import static org.mockito.Mockito.*;

class CryptoUtilTest {

    @Test
    void verifyRSASignReturnsTrueForValidSignature() throws Exception {
        Signature signature = mock(Signature.class);
        PublicKey key = mock(PublicKey.class);
        when(signature.verify(any(byte[].class))).thenReturn(true);

        Signature realSignature = Signature.getInstance("SHA256withRSA");
        Signature spySignature = spy(realSignature);
        doNothing().when(spySignature).initVerify(any(PublicKey.class));
        doNothing().when(spySignature).update(any(byte[].class));
        doReturn(true).when(spySignature).verify(any(byte[].class));

        try (var mocked = mockStatic(Signature.class)) {
            mocked.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(spySignature);
            boolean result = CryptoUtil.verifyRSASign("payload", new byte[]{1, 2, 3}, key, "SHA256withRSA");
            Assertions.assertTrue(result);
        }
    }

    @Test
    void verifyRSASignReturnsFalseForNoSuchAlgorithmException() {
        try (var mocked = mockStatic(Signature.class)) {
            mocked.when(() -> Signature.getInstance("bad-algo")).thenThrow(new NoSuchAlgorithmException());
            boolean result = CryptoUtil.verifyRSASign("payload", new byte[]{1}, mock(PublicKey.class), "bad-algo");
            Assertions.assertFalse(result);
        }
    }

    @Test
    void verifyRSASignReturnsFalseForInvalidKeyException() throws Exception {
        Signature signature = mock(Signature.class);
        doThrow(new InvalidKeyException()).when(signature).initVerify(any(PublicKey.class));
        try (var mocked = mockStatic(Signature.class)) {
            mocked.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(signature);
            boolean result = CryptoUtil.verifyRSASign("payload", new byte[]{1}, mock(PublicKey.class), "SHA256withRSA");
            Assertions.assertFalse(result);
        }
    }

    @Test
    void verifyRSASignReturnsFalseForSignatureException() throws Exception {
        Signature signature = mock(Signature.class);
        doNothing().when(signature).initVerify(any(PublicKey.class));
        doThrow(new SignatureException()).when(signature).update(any(byte[].class));
        try (var mocked = mockStatic(Signature.class)) {
            mocked.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(signature);
            boolean result = CryptoUtil.verifyRSASign("payload", new byte[]{1}, mock(PublicKey.class), "SHA256withRSA");
            Assertions.assertFalse(result);
        }
    }
}
