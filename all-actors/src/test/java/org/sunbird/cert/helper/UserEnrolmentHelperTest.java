package org.sunbird.cert.helper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class UserEnrolmentHelperTest {

    private CassandraOperation cassandraOperationMock;
    private UserEnrolmentHelper userEnrolmentHelper;
    private MockedStatic<ServiceFactory> mockedFactory;

    @BeforeAll
    void beforeAll() {
        cassandraOperationMock = mock(CassandraOperation.class);
        mockedFactory = mockStatic(ServiceFactory.class);
        mockedFactory.when(ServiceFactory::getInstance).thenReturn(cassandraOperationMock);
        userEnrolmentHelper = UserEnrolmentHelper.getInstance(); // instance initialized while static mock is active
    }

    @AfterAll
    void tearDown() {
        mockedFactory.close(); // Clean up
    }

    @Test
    void testGetUserEnrollmentRecord() throws BaseException {
        Response expected = new Response();
        when(cassandraOperationMock.getRecordsByProperties(anyString(), anyString(), anyMap()))
                .thenReturn(expected);

        Response result = userEnrolmentHelper.getUserEnrollmentRecord("course123", "batchA", "userX");

        assertNotNull(result);
        verify(cassandraOperationMock).getRecordsByProperties(eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                eq(JsonKeys.USER_ENROLMENTS), anyMap());
    }

    @Test
    void testGetUserEventEnrollmentRecord() throws BaseException {
        Response expected = new Response();
        when(cassandraOperationMock.getRecordsByProperties(anyString(), anyString(), anyMap()))
                .thenReturn(expected);

        Response result = userEnrolmentHelper.getUserEventEnrollmentRecord("course123", "batchA", "userX");

        assertNotNull(result);
        verify(cassandraOperationMock).getRecordsByProperties(eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                eq(JsonKeys.USER_ENTITY_ENROLMENTS), anyMap());
    }

    @Test
    void testUpdateUserEventEnrollmentRecord() throws BaseException {
        Response expected = new Response();
        when(cassandraOperationMock.updateRecord(anyString(), anyString(), anyMap(), anyMap()))
                .thenReturn(expected);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("progress", 50);

        Response result = userEnrolmentHelper.updateUserEventEnrollmentRecord("course123", "batchA", "userX", attributeMap);

        assertNotNull(result);
        verify(cassandraOperationMock).updateRecord(eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                eq(JsonKeys.USER_ENTITY_ENROLMENTS), eq(attributeMap), anyMap());
    }

    @Test
    void testUpdateUserEnrollmentRecord() throws BaseException {
        Response expected = new Response();
        when(cassandraOperationMock.updateRecord(anyString(), anyString(), anyMap(), anyMap()))
                .thenReturn(expected);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("status", "completed");

        Response result = userEnrolmentHelper.updateUserEnrollmentRecord("course123", "batchA", "userX", attributeMap);

        assertNotNull(result);
        verify(cassandraOperationMock).updateRecord(eq(JsonKeys.COURSE_KEY_SPACE_NAME),
                eq(JsonKeys.USER_ENROLMENTS), eq(attributeMap), anyMap());
    }
}
