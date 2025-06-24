package org.sunbird.incredible.pojos;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class TrainingEvidenceTest {

    @Test
    void testTrainingEvidenceGettersAndSetters() {
        TrainingEvidence evidence = new TrainingEvidence("https://example.org/context");

        // Verify context and type are initialized via constructor
        assertEquals("https://example.org/context", evidence.getContext());
        assertArrayEquals(new String[]{"Evidence", "Extension", "extensions:TrainingEvidence"}, evidence.getType());

        // Set and get subject
        String subject = "Advanced Java";
        evidence.setSubject(subject);
        assertEquals(subject, evidence.getSubject());

        // Set and get trainedBy
        String trainedBy = "https://example.org/trainer/profile";
        evidence.setTrainedBy(trainedBy);
        assertEquals(trainedBy, evidence.getTrainedBy());

        // Set and get session
        String session = "Spring2025-BatchA";
        evidence.setSession(session);
        assertEquals(session, evidence.getSession());
    }
}
