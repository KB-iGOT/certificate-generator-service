package org.sunbird.cert.actor;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VerificationResponseTest {

    @Test
    void validFlagIsSetAndRetrievedCorrectly() {
        VerificationResponse response = new VerificationResponse();
        response.setValid(Boolean.TRUE);
        assertTrue(response.getValid());
    }

    @Test
    void validIsNullByDefault() {
        VerificationResponse response = new VerificationResponse();
        assertNull(response.getValid());
    }

    @Test
    void messagesAreSetAndRetrievedCorrectly() {
        List<String> messages = List.of("msg1", "msg2");
        VerificationResponse response = new VerificationResponse();
        response.setMessages(messages);
        assertEquals(messages, response.getMessages());
    }

    @Test
    void messagesIsNullByDefault() {
        VerificationResponse response = new VerificationResponse();
        assertNull(response.getMessages());
    }

    @Test
    void errorCountIsSetAndRetrievedCorrectly() {
        VerificationResponse response = new VerificationResponse();
        response.setErrorCount(5);
        assertEquals(5, response.getErrorCount());
    }

    @Test
    void errorCountIsZeroByDefault() {
        VerificationResponse response = new VerificationResponse();
        assertEquals(0, response.getErrorCount());
    }
}
