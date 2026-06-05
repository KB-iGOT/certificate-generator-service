package org.sunbird.cert.helper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.response.Response;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class UserEnrolmentHelperCassandraTest {

    private CassandraOperation mockCassandra;

    @Before
    public void setup() {
        mockCassandra = Mockito.mock(CassandraOperation.class);
        UserEnrolmentHelper.setCassandraOperationForTest(mockCassandra);
    }

    @Test
    public void testGetUserExternalEnrollmentRecord_delegatesToCassandra() throws Exception {
        Response expected = new Response();
        expected.put(JsonKeys.RESPONSE, Arrays.asList(new HashMap<>()));

        Mockito.when(mockCassandra.getRecordsByProperties(
                Mockito.eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                Mockito.eq(JsonKeys.TABLE_USER_EXTERNAL_ENROLMENTS),
                Mockito.anyMap()
        )).thenReturn(expected);

        Response actual = UserEnrolmentHelper.getInstance().getUserExternalEnrollmentRecord("course-123", "user-456");

        Assert.assertSame(expected, actual);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(mockCassandra).getRecordsByProperties(
                Mockito.eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                Mockito.eq(JsonKeys.TABLE_USER_EXTERNAL_ENROLMENTS),
                captor.capture()
        );

        Map pk = captor.getValue();
        Assert.assertEquals("user-456", pk.get(JsonKeys.USER_ID_KEY));
        Assert.assertEquals("course-123", pk.get(JsonKeys.COURSE_ID_KEY));
    }

    @Test(expected = BaseException.class)
    public void testGetUserExternalEnrollmentRecord_propagatesBaseException() throws Exception {
        Mockito.when(mockCassandra.getRecordsByProperties(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()
        )).thenThrow(new BaseException("ERR", "db failure", 500));

        UserEnrolmentHelper.getInstance().getUserExternalEnrollmentRecord("c", "u");
    }

}



