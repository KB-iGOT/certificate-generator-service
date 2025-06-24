package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaTest {

    private Criteria criteria;

    @BeforeEach
    void setUp() {
        criteria = new Criteria();
    }

    @Test
    void testDefaultType() {
        assertArrayEquals(new String[]{"Criteria"}, criteria.getType());
    }

    @Test
    void testSetType() {
        String[] newType = {"Criteria", "https://example.org/type"};
        criteria.setType(newType);
        assertArrayEquals(newType, criteria.getType());
    }

    @Test
    void testIdGetterSetter() {
        criteria.setId("https://example.org/criteria/1");
        assertEquals("https://example.org/criteria/1", criteria.getId());
    }

    @Test
    void testNarrativeGetterSetter() {
        criteria.setNarrative("Complete the project and pass final evaluation.");
        assertEquals("Complete the project and pass final evaluation.", criteria.getNarrative());
    }

    @Test
    void testContextSetterGetter() {
        criteria.setContext("https://example.org/context");
        assertEquals("https://example.org/context", criteria.getContext());
    }

    @Test
    void testVersionSetterGetter() {
        criteria.setVersion("v1.2");
        assertEquals("v1.2", criteria.getVersion());
    }
}
