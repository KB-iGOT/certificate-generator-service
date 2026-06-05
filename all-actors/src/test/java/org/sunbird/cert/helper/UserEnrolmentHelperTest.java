package org.sunbird.cert.helper;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.JsonKeys;

import java.util.*;

public class UserEnrolmentHelperTest {

    @Test
    public void testGetActiveEnrollment_returnsFirstActive() {
        List<Map<String, Object>> enrollments = new ArrayList<>();
        enrollments.add(null);

        Map<String, Object> e1 = new HashMap<>();
        e1.put(JsonKeys.ACTIVE, Boolean.FALSE);
        e1.put("id", "e1");
        enrollments.add(e1);

        Map<String, Object> e2 = new HashMap<>();
        e2.put(JsonKeys.ACTIVE, Boolean.TRUE);
        e2.put("id", "e2");
        enrollments.add(e2);

        Map<String, Object> e3 = new HashMap<>();
        e3.put(JsonKeys.ACTIVE, Boolean.TRUE);
        e3.put("id", "e3");
        enrollments.add(e3);

        Map<String, Object> result = UserEnrolmentHelper.getInstance().getActiveEnrollment(enrollments);

        Assert.assertNotNull(result);
        Assert.assertEquals("e2", result.get("id"));
        Assert.assertTrue(Boolean.TRUE.equals(result.get(JsonKeys.ACTIVE)));
    }

    @Test
    public void testGetActiveEnrollment_returnsNullWhenNoActive() {
        List<Map<String, Object>> enrollments = new ArrayList<>();

        Map<String, Object> e1 = new HashMap<>();
        e1.put(JsonKeys.ACTIVE, Boolean.FALSE);
        e1.put("id", "e1");
        enrollments.add(e1);

        Map<String, Object> result = UserEnrolmentHelper.getInstance().getActiveEnrollment(enrollments);

        Assert.assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetActiveEnrollment_throwsWhenNullList() {
        UserEnrolmentHelper.getInstance().getActiveEnrollment(null);
    }

}

