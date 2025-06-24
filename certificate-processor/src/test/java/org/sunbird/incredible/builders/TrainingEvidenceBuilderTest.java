package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.TrainingEvidence;

import static org.junit.jupiter.api.Assertions.*;

class TrainingEvidenceBuilderTest {

    @Test
    void testTrainingEvidenceBuilder_allFields() {
        TrainingEvidenceBuilder builder = new TrainingEvidenceBuilder("https://example.org/context");
        TrainingEvidence evidence = builder
                .setId("https://example.org/evidence/123")
                .setNarrative("Completed advanced Java training")
                .setName("Java Developer Course")
                .setDescription("An in-depth Java certification course")
                .setAudience("Software Engineers")
                .setGenre("Technical")
                .setSubject("Java")
                .setTrainedBy("Expert Academy")
                .setSession("2024-W05")
                .build();

        assertNotNull(evidence);
        assertEquals("https://example.org/context", evidence.getContext());
        assertEquals("https://example.org/evidence/123", evidence.getId());
        assertEquals("Completed advanced Java training", evidence.getNarrative());
        assertEquals("Java Developer Course", evidence.getName());
        assertEquals("An in-depth Java certification course", evidence.getDescription());
        assertEquals("Software Engineers", evidence.getAudience());
        assertEquals("Technical", evidence.getGenre());
        assertEquals("Java", evidence.getSubject());
        assertEquals("Expert Academy", evidence.getTrainedBy());
        assertEquals("2024-W05", evidence.getSession());
    }
}
