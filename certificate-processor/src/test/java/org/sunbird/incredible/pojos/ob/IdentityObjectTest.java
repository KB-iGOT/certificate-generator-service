package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityObjectTest {

    @Test
    void testIdentityObjectSettersAndGetters() {
        IdentityObject identityObject = new IdentityObject();

        // Set values
        String testIdentity = "john.doe@example.com";
        String[] testType = {"email", "http://schema.org/email"};
        boolean testHashed = true;
        String testSalt = "s@ltV@lue";

        identityObject.setIdentity(testIdentity);
        identityObject.setType(testType);
        identityObject.setHashed(testHashed);
        identityObject.setSalt(testSalt);

        // Assert values
        assertEquals(testIdentity, identityObject.getIdentity());
        assertArrayEquals(testType, identityObject.getType());
        assertTrue(identityObject.isHashed());
        assertEquals(testSalt, identityObject.getSalt());
    }
}
