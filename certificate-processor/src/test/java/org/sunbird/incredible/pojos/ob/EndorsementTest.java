package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndorsementTest {

    private Endorsement endorsement;

    @BeforeEach
    void setUp() {
        endorsement = new Endorsement("https://example.org/context");
    }

    @Test
    void testContextInitialization() {
        assertEquals("https://example.org/context", endorsement.getContext());
    }

    @Test
    void testSetAndGetId() {
        String id = "urn:uuid:1234-5678";
        endorsement.setId(id);
        assertEquals(id, endorsement.getId());
    }

    @Test
    void testDefaultType() {
        assertArrayEquals(new String[]{"Endorsement"}, endorsement.getType());
    }

    @Test
    void testSetAndGetType() {
        String[] newType = new String[]{"CustomEndorsement", "Endorsement"};
        endorsement.setType(newType);
        assertArrayEquals(newType, endorsement.getType());
    }

    @Test
    void testSetAndGetClaim() {
        String claim = "Endorses skills in Java and Spring Boot";
        endorsement.setClaim(claim);
        assertEquals(claim, endorsement.getClaim());
    }

    @Test
    void testSetAndGetIssuer() {
        Profile profile = new Profile("");
        profile.setId("https://example.org/profile");
        endorsement.setIssuer(profile);
        assertEquals(profile, endorsement.getIssuer());
    }

    @Test
    void testSetAndGetIssuedOn() {
        String issuedDate = "2023-08-15T12:00:00Z";
        endorsement.setIssuedOn(issuedDate);
        assertEquals(issuedDate, endorsement.getIssuedOn());
    }

    @Test
    void testSetAndGetVerification() {
        VerificationObject verification = new VerificationObject();
        verification.setType(new String[]{"SignedBadge"});
        endorsement.setVerification(verification);
        assertEquals(verification, endorsement.getVerification());
    }

    @Test
    void testSetAndGetVersion() {
        endorsement.setVersion("1.0");
        assertEquals("1.0", endorsement.getVersion());
    }
}
