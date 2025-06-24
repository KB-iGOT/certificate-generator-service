package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.CompositeIdentityObject;
import org.sunbird.incredible.pojos.ob.exeptions.InvalidDateFormatException;

import static org.junit.jupiter.api.Assertions.*;

class AssertionTest {

    private Assertion assertion;

    @BeforeEach
    void setUp() {
        assertion = new Assertion("https://example.org/context");
    }

    @Test
    void testContextSetByConstructor() {
        assertEquals("https://example.org/context", assertion.getContext());
    }

    @Test
    void testIdGetterSetter() {
        assertion.setId("urn:uuid:1234");
        assertEquals("urn:uuid:1234", assertion.getId());
    }

    @Test
    void testTypeGetterSetter() {
        String[] types = {"Assertion", "https://w3.org/Assertion"};
        assertion.setType(types);
        assertArrayEquals(types, assertion.getType());
    }

    @Test
    void testIssuedOnValidFormat() throws InvalidDateFormatException {
        assertion.setIssuedOn("2020-01-01");
        assertEquals("2020-01-01T00:00:00Z", assertion.getIssuedOn());
    }

    @Test
    void testIssuedOnInvalidFormatThrowsException() {
        assertThrows(InvalidDateFormatException.class, () -> {
            assertion.setIssuedOn("invalid-date");
        });
    }

    @Test
    void testRecipientGetterSetter() {
        CompositeIdentityObject recipient = new CompositeIdentityObject();
        assertion.setRecipient(recipient);
        assertEquals(recipient, assertion.getRecipient());
    }

    @Test
    void testBadgeGetterSetter() {
        BadgeClass badge = new BadgeClass();
        assertion.setBadge(badge);
        assertEquals(badge, assertion.getBadge());
    }

    @Test
    void testImageGetterSetter() {
        assertion.setImage("https://example.com/image.png");
        assertEquals("https://example.com/image.png", assertion.getImage());
    }

    @Test
    void testEvidenceGetterSetter() {
        Evidence evidence = new Evidence();
        assertion.setEvidence(evidence);
        assertEquals(evidence, assertion.getEvidence());
    }

    @Test
    void testExpiresValidRelativeDate() throws InvalidDateFormatException {
        assertion.setIssuedOn("2020-01-01");
        assertion.setExpires("1d");
        assertEquals("2020-01-02T00:00:00Z", assertion.getExpires());
    }

    @Test
    void testExpiresInvalidThrowsException() throws InvalidDateFormatException {
        assertion.setIssuedOn("2020-01-01");
        assertThrows(InvalidDateFormatException.class, () -> {
            assertion.setExpires("2020-99-99");
        });
    }

    @Test
    void testVerificationGetterSetter() {
        VerificationObject verification = new VerificationObject();
        assertion.setVerification(verification);
        assertEquals(verification, assertion.getVerification());
    }

    @Test
    void testNarrativeGetterSetter() {
        assertion.setNarrative("This is the narrative");
        assertEquals("This is the narrative", assertion.getNarrative());
    }

    @Test
    void testRevokedGetterSetter() {
        assertion.setRevoked(true);
        assertTrue(assertion.isRevoked());
    }

    @Test
    void testRevocationReasonGetterSetter() {
        assertion.setRevocationReason("Fraudulent");
        assertEquals("Fraudulent", assertion.getRevocationReason());
    }
}
