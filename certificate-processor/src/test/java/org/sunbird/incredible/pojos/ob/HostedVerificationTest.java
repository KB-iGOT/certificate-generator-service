package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HostedVerificationTest {

    @Test
    void testHostedVerificationConstructorSetsTypeCorrectly() {
        HostedVerification hostedVerification = new HostedVerification();
        assertNotNull(hostedVerification.getType(), "Type should not be null");
        assertArrayEquals(new String[]{"HostedBadge"}, hostedVerification.getType());
    }

}
