package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceTest {

    private Evidence evidence;

    @BeforeEach
    void setUp() {
        evidence = new Evidence("https://example.org/context");
    }

    @Test
    void testContextInitialization() {
        assertEquals("https://example.org/context", evidence.getContext());
    }

    @Test
    void testSetAndGetId() {
        String id = "urn:uuid:abcd-1234";
        evidence.setId(id);
        assertEquals(id, evidence.getId());
    }

    @Test
    void testSetAndGetType() {
        String[] types = new String[]{"Certificate", "Video"};
        evidence.setType(types);
        assertArrayEquals(types, evidence.getType());
    }

    @Test
    void testSetAndGetNarrative() {
        String narrative = "Completed capstone project with distinction.";
        evidence.setNarrative(narrative);
        assertEquals(narrative, evidence.getNarrative());
    }

    @Test
    void testSetAndGetName() {
        String name = "Capstone Project";
        evidence.setName(name);
        assertEquals(name, evidence.getName());
    }

    @Test
    void testSetAndGetDescription() {
        String description = "A hands-on project on full-stack development.";
        evidence.setDescription(description);
        assertEquals(description, evidence.getDescription());
    }

    @Test
    void testSetAndGetGenre() {
        String genre = "Project";
        evidence.setGenre(genre);
        assertEquals(genre, evidence.getGenre());
    }

    @Test
    void testSetAndGetAudience() {
        String audience = "Hiring managers and potential employers.";
        evidence.setAudience(audience);
        assertEquals(audience, evidence.getAudience());
    }

    @Test
    void testSetAndGetVersion() {
        evidence.setVersion("1.1");
        assertEquals("1.1", evidence.getVersion());
    }
}
