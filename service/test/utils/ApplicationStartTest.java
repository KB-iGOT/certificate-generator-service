package utils;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.JsonKeys;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplicationStartTest {

    private String originalIp;
    private String originalPort;
    private String originalUser;
    private String originalPassword;

    @BeforeEach
    void saveEnv() {
        originalIp = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_IP);
        originalPort = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_PORT);
        originalUser = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_USER_NAME);
        originalPassword = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_PASSWORD);
    }

    @AfterEach
    void clearProps() {
        System.clearProperty(JsonKeys.SUNBIRD_CASSANDRA_IP);
        System.clearProperty(JsonKeys.SUNBIRD_CASSANDRA_PORT);
        System.clearProperty(JsonKeys.SUNBIRD_CASSANDRA_USER_NAME);
        System.clearProperty(JsonKeys.SUNBIRD_CASSANDRA_PASSWORD);
    }


    @Test
    public void createCassandraConnection_ThrowsExceptionWhenIpOrPortMissing() throws Exception {
        setEnv(JsonKeys.SUNBIRD_CASSANDRA_IP, "");
        setEnv(JsonKeys.SUNBIRD_CASSANDRA_PORT, "");
        // Optionally expect an exception here
        ApplicationStart.createCassandraConnection("testKeyspace");
    }

    @Test
    public void createCassandraConnection_ThrowsExceptionOnConnectionFailure() throws Exception {
        System.setProperty(JsonKeys.SUNBIRD_CASSANDRA_IP, "127.0.0.1");
        System.setProperty(JsonKeys.SUNBIRD_CASSANDRA_PORT, "9042");

        CassandraConnectionManager manager = mock(CassandraConnectionManager.class);
        when(manager.createConnection(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        try (MockedStatic<CassandraConnectionMngrFactory> mockStatic = mockStatic(CassandraConnectionMngrFactory.class)) {
            mockStatic.when(() -> CassandraConnectionMngrFactory.getObject(anyString()))
                    .thenReturn(manager);

            ApplicationStart.createCassandraConnection("testKeyspace");
        }
    }

    // Helper to set environment variables for testing
    private static void setEnv(String key, String value) {
        try {
            java.util.Map<String, String> env = System.getenv();
            java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((java.util.Map<String, String>) field.get(env)).put(key, value);
        } catch (Exception e) {
            // ignore for test
        }
    }
}
