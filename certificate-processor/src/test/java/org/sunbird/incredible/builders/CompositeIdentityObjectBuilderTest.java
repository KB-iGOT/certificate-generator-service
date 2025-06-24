package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.CompositeIdentityObject;
import org.sunbird.incredible.pojos.Gender;

import static org.junit.jupiter.api.Assertions.*;

class CompositeIdentityObjectBuilderTest {

    @Test
    void testDefaultConstructorAndBuild() {
        CompositeIdentityObjectBuilder builder = new CompositeIdentityObjectBuilder();
        CompositeIdentityObject object = builder
                .setName("John Doe")
                .setdob("1990-01-01")
                .setGender(Gender.MALE)
                .setUrn("urn:example:1234")
                .setTag("Student")
                .setPhoto("http://example.org/photo.jpg")
                .setUrl("http://example.org/profile")
                .setId("did:example:123")
                .setType(new String[]{"Person", "Identity"})
                .setHashed(true)
                .build();

        assertNotNull(object);
        assertEquals("John Doe", object.getName());
        assertEquals("1990-01-01", object.getDob());
        assertEquals(Gender.MALE, object.getGender());
        assertEquals("urn:example:1234", object.getUrn());
        assertEquals("Student", object.getTag());
        assertEquals("http://example.org/photo.jpg", object.getPhoto());
        assertEquals("http://example.org/profile", object.getUrl());
        assertEquals("did:example:123", object.getIdentity());
        assertArrayEquals(new String[]{"Person", "Identity"}, object.getType());
    }

    @Test
    void testConstructorWithContext() {
        CompositeIdentityObjectBuilder builder = new CompositeIdentityObjectBuilder("https://schema.org");
        CompositeIdentityObject object = builder
                .setName("Alice")
                .build();

        assertNotNull(object);
        assertEquals("Alice", object.getName());
        assertEquals("https://schema.org", object.getContext());
    }
}
