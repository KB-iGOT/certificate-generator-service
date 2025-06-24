package org.sunbird.incredible.pojos.ob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OBBaseTest {

    @Test
    void testAllGettersAndSetters() {
        OBBase obBase = new OBBase();

        String context = "https://example.org/context";
        String[] related = new String[]{"related-1", "related-2"};
        String version = "v2.0";

        Endorsement endorsement = new Endorsement("https://endorsement.context");
        endorsement.setClaim("Trusted Entity");

        obBase.setContext(context);
        obBase.setRelated(related);
        obBase.setVersion(version);
        obBase.setEndorsement(endorsement);

        assertEquals(context, obBase.getContext());
        assertArrayEquals(related, obBase.getRelated());
        assertEquals(version, obBase.getVersion());
        assertEquals(endorsement, obBase.getEndorsement());
    }

    @Test
    void testToStringReturnsValidJson() {
        OBBase obBase = new OBBase();
        obBase.setContext("https://example.org/context");
        obBase.setVersion("1.0");
        obBase.setRelated(new String[]{"related-1"});

        String json = obBase.toString();

        assertNotNull(json);
        assertTrue(json.contains("context"));
        assertTrue(json.contains("1.0"));
    }
}
