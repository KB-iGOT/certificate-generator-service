package org.sunbird.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;


import com.datastax.driver.core.*;
import org.mockito.Mockito;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.message.Localizer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class CassandraConnectionManagerImplTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    Cluster mockCluster;
    Session mockSession;
    Metadata mockMetadata;
    Host mockHost;
    KeyspaceMetadata keyspaceMetadata;
    TableMetadata table1;
    TableMetadata table2;

    @BeforeEach
    public void setup() {
        mockCluster = mock(Cluster.class);
        mockSession = mock(Session.class);
        mockMetadata = mock(Metadata.class);
        mockHost = mock(Host.class);
        keyspaceMetadata = mock(KeyspaceMetadata.class);
        table1 = mock(TableMetadata.class);
        table2 = mock(TableMetadata.class);
    }

    @Test
    void testGetConsistencyLevel_validValue() throws Exception {
        try (MockedStatic<PropertiesCache> mockedCache = mockStatic(PropertiesCache.class)) {
            mockedCache.when(() -> PropertiesCache.getConfigValue(anyString()))
                    .thenReturn("QUORUM");

            Method method = CassandraConnectionManagerImpl.class.getDeclaredMethod("getConsistencyLevel");
            method.setAccessible(true);
            ConsistencyLevel level = (ConsistencyLevel) method.invoke(null);

            assertEquals(ConsistencyLevel.QUORUM, level);
        }
    }


    @Test
    void testGetConsistencyLevel_invalidValue() throws Exception {
        try (MockedStatic<PropertiesCache> mockedCache = mockStatic(PropertiesCache.class)) {
            mockedCache.when(() -> PropertiesCache.getConfigValue(anyString()))
                    .thenReturn("INVALID_LEVEL");

            Method method = CassandraConnectionManagerImpl.class.getDeclaredMethod("getConsistencyLevel");
            method.setAccessible(true);
            ConsistencyLevel level = (ConsistencyLevel) method.invoke(null);

            assertNull(level);
        }
    }

    @Test
    void testCreateConnection_failure_dueToException() {
        CassandraConnectionManagerImpl impl = new CassandraConnectionManagerImpl("standalone");
        assertThrows(BaseException.class, () ->
                impl.createConnection("127.0.0.1", "9042", null, null, "test_keyspace")
        );
    }

    @Test
    void testGetCluster_clusterNull() {
        CassandraConnectionManagerImpl impl = new CassandraConnectionManagerImpl("standalone");
        assertThrows(BaseException.class, () -> impl.getCluster("non_existing"));
    }

    @Test
    void testCreateCassandraConnection_configMissing() throws Exception {
        // Simulate missing env variables
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_IP, null);
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_PORT, null);

        boolean result = CassandraConnectionManagerImpl.createCassandraConnection("testKeyspace");

        assertFalse(result); // Because connection config is missing
    }

    @Test
    void testCreateCassandraConnection_failureInConnection() throws BaseException {
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_IP, "127.0.0.1");
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_PORT, "9042");
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_USER_NAME, null);
        environmentVariables.set(JsonKeys.SUNBIRD_CASSANDRA_PASSWORD, null);

        try (MockedStatic<CassandraConnectionMngrFactory> mockedFactory = mockStatic(CassandraConnectionMngrFactory.class)) {
            CassandraConnectionManager mockMgr = mock(CassandraConnectionManager.class);
            when(mockMgr.createConnection(any(), any(), any(), any(), any())).thenReturn(false);

            mockedFactory.when(() -> CassandraConnectionMngrFactory.getObject(JsonKeys.STANDALONE_MODE))
                    .thenReturn(mockMgr);

            assertThrows(BaseException.class,
                    () -> CassandraConnectionManagerImpl.createCassandraConnection("test"));
        }
    }


    @Test
    void testGetLocalizedMessage() throws Exception {
        // Mock Localizer
        Localizer mockLocalizer = mock(Localizer.class);
        when(mockLocalizer.getMessage(eq("test"), any())).thenReturn("Mocked message");

        // Use reflection to inject the mock into CassandraConnectionManagerImpl
        Field localizerField = CassandraConnectionManagerImpl.class.getDeclaredField("localizer");
        localizerField.setAccessible(true);
        localizerField.set(null, mockLocalizer); // static field

        // Call private static method via reflection
        Method method = CassandraConnectionManagerImpl.class.getDeclaredMethod(
                "getLocalizedMessage", String.class, Locale.class);
        method.setAccessible(true);
        Object result = method.invoke(null, "test", null);

        assertEquals("Mocked message", result);
    }


    @Test
    void testGetTableList_shouldReturnTableNames() throws NoSuchFieldException, IllegalAccessException {
        Field clusterMapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraclusterMap");
        clusterMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Cluster> clusterMap = (Map<String, Cluster>) clusterMapField.get(null);
        clusterMap.clear();
        clusterMap.put("test_keyspace", mockCluster);

        // Given
        when(mockCluster.getMetadata()).thenReturn(mockMetadata);
        when(mockMetadata.getKeyspace("test_keyspace")).thenReturn(keyspaceMetadata);
        when(keyspaceMetadata.getTables()).thenReturn(List.of(table1, table2));
        when(table1.getName()).thenReturn("user");
        when(table2.getName()).thenReturn("course");
        CassandraConnectionManagerImpl cassandraConnectionManager = new CassandraConnectionManagerImpl("standalone");
        // When
        List<String> result = cassandraConnectionManager.getTableList("test_keyspace");

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("user"));
        assertTrue(result.contains("course"));
    }


    @Test
    void testGetSession_sessionAlreadyExists_shouldReturnIt() throws BaseException, NoSuchFieldException, IllegalAccessException {
        Field sessionMapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraSessionMap");
        sessionMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Session> sessionMap = (Map<String, Session>) sessionMapField.get(null);
        sessionMap.clear();
        sessionMap.put("test_keyspace", mockSession);
        // When
        CassandraConnectionManagerImpl cassandraConnectionManager = new CassandraConnectionManagerImpl("standalone");

        Session session = cassandraConnectionManager.getSession("test_keyspace");

        // Then
        assertNotNull(session);
        assertEquals(mockSession, session);
    }

    @Test
    void testGetSession_sessionMissing_shouldInvokeCreateCassandraConnection() throws Exception {

        // Clear session map
        Field sessionMapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraSessionMap");
        sessionMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Session> sessionMap = (Map<String, Session>) sessionMapField.get(null);
        sessionMap.clear();

        // Inject mock session after createCassandraConnection
        try (MockedStatic<CassandraConnectionManagerImpl> mockedStatic = Mockito.mockStatic(CassandraConnectionManagerImpl.class, Mockito.CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> CassandraConnectionManagerImpl.createCassandraConnection("test_keyspace")).thenAnswer(invocation -> {
                sessionMap.put("test_keyspace", mockSession);
                return true;
            });
            CassandraConnectionManagerImpl cassandraConnectionManager = new CassandraConnectionManagerImpl("standalone");

            Session session = cassandraConnectionManager.getSession("test_keyspace");

            assertNotNull(session);
            assertEquals(mockSession, session);
        }
    }
}
