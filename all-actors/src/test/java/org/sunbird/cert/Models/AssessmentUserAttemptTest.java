package org.sunbird.cert.Models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssessmentUserAttemptTest {

    @Test
    void constructorInitializesFieldsCorrectly() {
        AssessmentUserAttempt attempt = new AssessmentUserAttempt("content123", 8.5, 10.0);
        assertEquals("content123", attempt.getContentId());
        assertEquals(8.5, attempt.getScore());
        assertEquals(10.0, attempt.getTotalScore());
    }

    @Test
    void handlesZeroAndNegativeScores() {
        AssessmentUserAttempt attempt = new AssessmentUserAttempt("zero", 0.0, 0.0);
        assertEquals(0.0, attempt.getScore());
        assertEquals(0.0, attempt.getTotalScore());

        AssessmentUserAttempt negativeAttempt = new AssessmentUserAttempt("neg", -5.0, -10.0);
        assertEquals(-5.0, negativeAttempt.getScore());
        assertEquals(-10.0, negativeAttempt.getTotalScore());
    }

    @Test
    void handlesNullContentId() {
        AssessmentUserAttempt attempt = new AssessmentUserAttempt(null, 1.0, 2.0);
        assertNull(attempt.getContentId());
    }
}
