package org.sunbird.incredible.pojos;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeIdentityObjectTest {

    @Test
    void testGettersAndSetters() {
        CompositeIdentityObject obj = new CompositeIdentityObject();

        // components
        CompositeIdentityObject comp1 = new CompositeIdentityObject();
        CompositeIdentityObject comp2 = new CompositeIdentityObject();
        List<CompositeIdentityObject> components = Arrays.asList(comp1, comp2);
        obj.setComponents(components);
        assertEquals(components, obj.getComponents());

        // name
        String name = "Test Name";
        obj.setName(name);
        assertEquals(name, obj.getName());

        // photo
        String photo = "data:image/jpeg;base64,...";
        obj.setPhoto(photo);
        assertEquals(photo, obj.getPhoto());

        // dob
        String dob = "1990-01-01";
        obj.setDob(dob);
        assertEquals(dob, obj.getDob());

        // gender
        Gender gender = Gender.MALE;
        obj.setGender(gender);
        assertEquals(gender, obj.getGender());

        // tag
        String tag = "tag:example:123";
        obj.setTag(tag);
        assertEquals(tag, obj.getTag());

        // urn
        String urn = "urn:example:456";
        obj.setUrn(urn);
        assertEquals(urn, obj.getUrn());

        // url
        String url = "https://example.com";
        obj.setUrl(url);
        assertEquals(url, obj.getUrl());

        // type should be pre-set
        assertArrayEquals(new String[]{"Extension", "IdentityObject", "extensions:CompositeIdentityObject"}, obj.getType());
    }

    @Test
    void testInheritanceFromIdentityObject() {
        CompositeIdentityObject obj = new CompositeIdentityObject();

        // IdentityObject inherited properties
        String identity = "user123";
        obj.setIdentity(identity);
        assertEquals(identity, obj.getIdentity());

        String[] types = {"type1", "type2"};
        obj.setType(types);
        assertArrayEquals(types, obj.getType());

        boolean hashed = true;
        obj.setHashed(hashed);
        assertTrue(obj.isHashed());

        String salt = "randomSalt";
        obj.setSalt(salt);
        assertEquals(salt, obj.getSalt());
    }
}
