package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlignmentObjectTest {

    private AlignmentObject alignmentObject;

    @BeforeEach
    void setUp() {
        alignmentObject = new AlignmentObject();
    }

    @Test
    void testTargetNameSetterGetter() {
        alignmentObject.setTargetName("Skill Alignment");
        assertEquals("Skill Alignment", alignmentObject.getTargetName());
    }

    @Test
    void testTargetURLSetterGetter() {
        alignmentObject.setTargetURL("https://example.com/align");
        assertEquals("https://example.com/align", alignmentObject.getTargetURL());
    }

    @Test
    void testTargetDescriptionSetterGetter() {
        alignmentObject.setTargetDescription("Describes alignment goal");
        assertEquals("Describes alignment goal", alignmentObject.getTargetDescription());
    }

    @Test
    void testTargetFrameworkSetterGetter() {
        alignmentObject.setTargetFramework("Framework A");
        assertEquals("Framework A", alignmentObject.getTargetFramework());
    }

    @Test
    void testTargetCodeSetterGetter() {
        alignmentObject.setTargetCode("SKL-001");
        assertEquals("SKL-001", alignmentObject.getTargetCode());
    }
}
