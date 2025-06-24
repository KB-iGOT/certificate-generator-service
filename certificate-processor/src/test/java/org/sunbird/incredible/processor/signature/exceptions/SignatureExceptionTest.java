package org.sunbird.incredible.processor.signature.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignatureExceptionTest {

    @Test
    void testCreationExceptionMessage() {
        SignatureException outer = new SignatureException();
        SignatureException.CreationException ex = outer.new CreationException("Test creation");
        assertEquals("Unable to create signature: Test creation", ex.getMessage());
    }

    @Test
    void testVerificationExceptionMessage() {
        SignatureException outer = new SignatureException();
        SignatureException.VerificationException ex = outer.new VerificationException("Test verify");
        assertEquals("Unable to verify signature Test verify", ex.getMessage());
    }

    @Test
    void testUnreachableExceptionMessage() {
        SignatureException outer = new SignatureException();
        SignatureException.UnreachableException ex = outer.new UnreachableException("Service down");
        assertEquals("Unable to reach service: Service down", ex.getMessage());
    }

    @Test
    void testKeyNotFoundExceptionMessage() {
        SignatureException outer = new SignatureException();
        SignatureException.KeyNotFoundException ex = outer.new KeyNotFoundException("Key missing");
        assertEquals("Unable to get key: Key missing", ex.getMessage());
    }
}
