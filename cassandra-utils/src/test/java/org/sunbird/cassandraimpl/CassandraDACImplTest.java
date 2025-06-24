package org.sunbird.cassandraimpl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.FutureCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.BaseException;
import org.sunbird.common.CassandraUtil;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.response.Response;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CassandraDACImplTest {

    @InjectMocks
    private CassandraDACImpl cassandraDAC;

    @Mock
    private Session mockSession;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private ResultSetFuture mockFuture;

    @Captor
    private ArgumentCaptor<Select> selectCaptor;

    @BeforeEach
    void setUp() throws BaseException {
        MockitoAnnotations.openMocks(this);
        cassandraDAC.connectionManager = mock(CassandraConnectionManagerImpl.class);
        when(cassandraDAC.connectionManager.getSession(anyString())).thenReturn(mockSession);
    }

    @Test
    void testGetRecords_withException() {
        when(mockSession.execute(any(Statement.class))).thenThrow(new RuntimeException("Cassandra failure"));

        Map<String, Object> filters = new HashMap<>();
        filters.put("id", "user123");

        assertThrows(BaseException.class, () -> cassandraDAC.getRecords("keyspace", "table", filters, null));
    }

    @Test
    void testApplyOperationOnRecordsAsync_successful() throws BaseException {
        when(mockSession.executeAsync(any(Statement.class))).thenReturn(mockFuture);

        FutureCallback<ResultSet> callback = new FutureCallback<>() {
            public void onSuccess(ResultSet result) {}
            public void onFailure(Throwable t) {}
        };

        cassandraDAC.applyOperationOnRecordsAsync("keyspace", "table", null, null, callback);
        verify(mockSession).executeAsync(any(Statement.class));
    }

    @Test
    void testApplyOperationOnRecordsAsync_exception() {
        when(mockSession.executeAsync(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        FutureCallback<ResultSet> callback = new FutureCallback<>() {
            public void onSuccess(ResultSet result) {}
            public void onFailure(Throwable t) {}
        };
        assertThrows(BaseException.class, () ->
                cassandraDAC.applyOperationOnRecordsAsync("keyspace", "table", null, null, callback));
    }

    @Test
    void testUpdateAddMapRecord_success() throws BaseException {
        Map<String, Object> pk = new HashMap<>();
        pk.put("id", "user123");

        when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

        Response response = cassandraDAC.updateAddMapRecord("keyspace", "table", pk, "map_column", "key1", "val1");
        assertEquals("SUCCESS", response.getResult().get("response"));
    }

    @Test
    void testUpdateRemoveMapRecord_success() throws BaseException {
        Map<String, Object> pk = new HashMap<>();
        pk.put("id", "user123");

        when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

        Response response = cassandraDAC.updateRemoveMapRecord("keyspace", "table", pk, "map_column", "key1");
        assertEquals("SUCCESS", response.getResult().get("response"));
    }

    @Test
    void testUpdateMapRecord_missingPrimaryKey_shouldThrowException() {
        assertThrows(BaseException.class, () ->
                cassandraDAC.updateAddMapRecord("keyspace", "table", null, "map_column", "key1", "val1")
        );
    }

    @Test
    void testUpdateMapRecord_withException() {
        Map<String, Object> pk = new HashMap<>();
        pk.put("id", "user123");
        when(mockSession.execute(any(Statement.class))).thenThrow(new RuntimeException("Cassandra down"));

        assertThrows(BaseException.class, () ->
                cassandraDAC.updateRemoveMapRecord("keyspace", "table", pk, "map_column", "key1")
        );
    }

    private void mockStaticResponse() {
        // Simulate static utility method Response returned from CassandraUtil
        mockStatic(CassandraUtil.class);
        when(CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(new Response());
    }
}
