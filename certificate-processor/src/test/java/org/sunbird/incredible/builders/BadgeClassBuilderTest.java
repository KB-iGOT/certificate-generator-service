package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.ob.*;

import static org.junit.jupiter.api.Assertions.*;

class BadgeClassBuilderTest {

    @Test
    void testBadgeClassBuilderAllFields() {
        String context = "https://example.org/context";
        String id = "https://example.org/badge";
        String[] type = new String[]{"BadgeClass", "Extension"};
        String name = "Certified Java Developer";
        String description = "Awarded for completing Java certification.";
        String image = "https://example.org/image.png";

        Criteria criteria = new Criteria();
        criteria.setId("https://example.org/criteria");
        criteria.setNarrative("Completed course and passed exam");

        Issuer issuer = new Issuer("https://example.org/issuer");
        issuer.setId("https://example.org/issuer");
        issuer.setName("Sunbird Academy");

        AlignmentObject alignment = new AlignmentObject();
        alignment.setTargetName("Java SE");
        alignment.setTargetCode("JAVA-101");

        BadgeClass badgeClass = new BadgeClassBuilder(context)
                .setId(id)
                .setType(type)
                .setName(name)
                .setDescription(description)
                .setImage(image)
                .setCriteria(criteria)
                .setIssuer(issuer)
                .setAlignment(alignment)
                .build();

        assertEquals(id, badgeClass.getId());
        assertArrayEquals(type, badgeClass.getType());
        assertEquals(name, badgeClass.getName());
        assertEquals(description, badgeClass.getDescription());
        assertEquals(image, badgeClass.getImage());
        assertEquals(criteria, badgeClass.getCriteria());
        assertEquals(issuer, badgeClass.getIssuer());
        assertEquals(alignment, badgeClass.getAlignment());
        assertEquals(context, badgeClass.getContext());
    }
}
