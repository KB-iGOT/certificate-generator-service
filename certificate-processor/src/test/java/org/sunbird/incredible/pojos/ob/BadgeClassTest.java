package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BadgeClassTest {

    private BadgeClass badgeClass;

    @BeforeEach
    void setUp() {
        badgeClass = new BadgeClass();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(badgeClass);
    }

    @Test
    void testConstructorWithContext() {
        BadgeClass badgeWithCtx = new BadgeClass("https://example.org/context");
        assertEquals("https://example.org/context", badgeWithCtx.getContext());
    }

    @Test
    void testIdGetterSetter() {
        badgeClass.setId("urn:uuid:badge-123");
        assertEquals("urn:uuid:badge-123", badgeClass.getId());
    }

    @Test
    void testTypeGetterDefault() {
        assertArrayEquals(new String[]{"BadgeClass"}, badgeClass.getType());
    }

    @Test
    void testTypeSetter() {
        String[] types = {"BadgeClass", "https://example.org/type"};
        badgeClass.setType(types);
        assertArrayEquals(types, badgeClass.getType());
    }

    @Test
    void testNameGetterSetter() {
        badgeClass.setName("Java Mastery Badge");
        assertEquals("Java Mastery Badge", badgeClass.getName());
    }

    @Test
    void testDescriptionGetterSetter() {
        badgeClass.setDescription("Awarded for mastering Java basics.");
        assertEquals("Awarded for mastering Java basics.", badgeClass.getDescription());
    }

    @Test
    void testVersionGetterSetter() {
        badgeClass.setVersion("v1.0.1");
        assertEquals("v1.0.1", badgeClass.getVersion());
    }

    @Test
    void testImageGetterSetter() {
        badgeClass.setImage("https://example.org/image.png");
        assertEquals("https://example.org/image.png", badgeClass.getImage());
    }

    @Test
    void testCriteriaGetterSetter() {
        Criteria criteria = new Criteria();
        badgeClass.setCriteria(criteria);
        assertEquals(criteria, badgeClass.getCriteria());
    }

    @Test
    void testIssuerGetterSetter() {
        Issuer issuer = new Issuer("");
        badgeClass.setIssuer(issuer);
        assertEquals(issuer, badgeClass.getIssuer());
    }

    @Test
    void testAlignmentGetterSetter() {
        AlignmentObject alignment = new AlignmentObject();
        badgeClass.setAlignment(alignment);
        assertEquals(alignment, badgeClass.getAlignment());
    }
}
