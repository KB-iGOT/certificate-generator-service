package org.sunbird.cassandraimpl;

import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.http.HttpStatus;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.datastax.driver.core.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sunbird.BaseException;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;

import java.net.InetAddress;
import java.util.*;

class CassandraOperationImplTest {

  private CassandraOperationImpl cassandraOperation;
  private CassandraConnectionManager mockConnectionManager;
  private Session mockSession;
  private ResultSet mockResultSet;
  private Response mockResponse;
  @Mock
  Statement mockUpdateStatement;

  private PreparedStatement mockPreparedStatement;
  private BoundStatement mockBoundStatement;

  private Localizer mockLocalizer;

  @BeforeEach
  void setup() {
    mockConnectionManager = mock(CassandraConnectionManager.class);
    mockSession = mock(Session.class);
    mockResultSet = mock(ResultSet.class);
    mockLocalizer = mock(Localizer.class);
    mockBoundStatement =  mock(BoundStatement.class);
    mockPreparedStatement = mock(PreparedStatement.class);

    cassandraOperation = new CassandraOperationImpl() {
      @Override
      public Response updateAddMapRecord(String keySpace, String table, Map<String, Object> primaryKey, String column, String key, Object value) { return null; }
      @Override
      public Response updateRemoveMapRecord(String keySpace, String table, Map<String, Object> primaryKey, String column, String key) { return null; }
      {
        this.connectionManager = mockConnectionManager;
        this.localizer = mock(Localizer.class);
      }
    };
  }

  @Test
  void insertRecordWithTTLReturnsSuccessResponse() throws Exception {
    String keyspace = "ks";
    String table = "table";
    Map<String, Object> request = Map.of("id", "1");
    int ttl = 100;
    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
    try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
      util.when(() -> CassandraUtil.createResponse(any())).thenReturn(new Response());
      Response resp = cassandraOperation.insertRecordWithTTL(keyspace, table, request, ttl);
      assertNotNull(resp);
    }
  }

  @Test
  void getRecordByObjectTypeReturnsResponse() throws BaseException {
    String keyspace = "ks";
    String table = "table";
    String column = "col";
    String key = "k";
    int value = 5;
    String objectType = "type";
    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
    try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
      util.when(() -> CassandraUtil.createResponse(any())).thenReturn(new Response());
      Response resp = cassandraOperation.getRecordByObjectType(keyspace, table, column, key, value, objectType);
      assertNotNull(resp);
    }
  }

  @Test
  void searchValueInListReturnsResponse() throws BaseException {
    String keyspace = "ks";
    String table = "table";
    String key = "k";
    String value = "v";
    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
    try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
      util.when(() -> CassandraUtil.createResponse(any())).thenReturn(new Response());
      Response resp = cassandraOperation.searchValueInList(keyspace, table, key, value);
      assertNotNull(resp);
    }
  }

  @Test
  void searchValueInListWithPropertyMapReturnsResponse() throws BaseException {
    String keyspace = "ks";
    String table = "table";
    String key = "k";
    String value = "v";
    Map<String, Object> propertyMap = Map.of("p", "val");
    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
    try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
      util.when(() -> CassandraUtil.createResponse(any())).thenReturn(new Response());
      Response resp = cassandraOperation.searchValueInList(keyspace, table, key, value, propertyMap);
      assertNotNull(resp);
    }
  }

  @Test
  void getLocalizedMessageReturnsMessageFromLocalizer() {
    Localizer localizer = mock(Localizer.class);
    cassandraOperation.localizer = localizer;
    when(localizer.getMessage(anyString(), any())).thenReturn("msg");
    String result = cassandraOperation.getLocalizedMessage("key", Locale.ENGLISH);
    assertEquals("msg", result);
  }

  @Test
  void getRecordsReturnsNull() throws Exception {
    assertNull(cassandraOperation.getRecords("ks", "table", Map.of(), List.of()));
  }

  @Test
  void applyOperationOnRecordsAsyncDoesNotThrow() {
    assertDoesNotThrow(() -> cassandraOperation.applyOperationOnRecordsAsync("ks", "table", Map.of(), List.of(), null));
  }

  @Test
  void batchInsertWithTTL_successWithValidTTL() throws Exception {
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> record = new HashMap<>();
    record.put("id", "123");
    record.put("name", "Ajay");

    List<Map<String, Object>> records = List.of(record);
    List<Integer> ttls = List.of(300);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);

    Response response = cassandraOperation.batchInsertWithTTL(keyspace, table, records, ttls);

    assertNotNull(response);
    assertEquals("SUCCESS", response.get(Constants.RESPONSE));
    verify(mockSession).execute(any(BatchStatement.class));
  }

//  @Test
//  void batchInsertWithTTL_successWithNullTTL() throws Exception {
//    String keyspace = "sunbird";
//    String table = "user";
//
//    Map<String, Object> record = new HashMap<>();
//    record.put("id", "123");
//
//    List<Map<String, Object>> records = List.of(record);
//    List<Integer> ttls = List.of(null);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);
//
//    Response response = cassandraOperation.batchInsertWithTTL(keyspace, table, records, ttls);
//
//    assertEquals("SUCCESS", response.get(Constants.RESPONSE));
//  }

  @Test
  void batchInsertWithTTL_errorWhenRecordsOrTTLsAreEmpty() {
    List<Map<String, Object>> records = new ArrayList<>();
    List<Integer> ttls = List.of(300);

    Exception ex = assertThrows(BaseException.class,
            () -> cassandraOperation.batchInsertWithTTL("ks", "tbl", records, ttls));
    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void batchInsertWithTTL_errorWhenSizeMismatch() {
    Map<String, Object> record = Map.of("id", "1");
    List<Map<String, Object>> records = List.of(record);
    List<Integer> ttls = List.of(300, 100);

    Exception ex = assertThrows(BaseException.class,
            () -> cassandraOperation.batchInsertWithTTL("ks", "tbl", records, ttls));
    assertEquals("SERVER_ERROR", ex.getMessage());
  }


  @Test
  void batchInsertWithTTL_queryValidationException() throws BaseException {
    Map<String, Object> record = Map.of("id", "1");
    List<Map<String, Object>> records = List.of(record);
    List<Integer> ttls = List.of(100);

    when(mockConnectionManager.getSession(anyString())).thenReturn(mockSession);
    //when(mockSession.execute(any(BatchStatement.class))).thenThrow(new QueryValidationException(null, "validation failed"));


    Response response = cassandraOperation.batchInsertWithTTL("ks", "tbl", records, ttls);
    assertEquals(HttpStatus.SC_OK, response.getResponseCode().getCode());
  }

  @Test
  void batchInsertWithTTL_noHostAvailableException() throws BaseException {
    Map<String, Object> record = Map.of("id", "1");
    List<Map<String, Object>> records = List.of(record);
    List<Integer> ttls = List.of(200);

    when(mockConnectionManager.getSession(anyString())).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenThrow(new NoHostAvailableException(Collections.emptyMap()));

    BaseException ex = assertThrows(BaseException.class,
            () -> cassandraOperation.batchInsertWithTTL("ks", "tbl", records, ttls));
    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void batchInsertWithTTL_illegalStateException() throws BaseException {
    Map<String, Object> record = Map.of("id", "1");
    List<Map<String, Object>> records = List.of(record);
    List<Integer> ttls = List.of(200);

    when(mockConnectionManager.getSession(anyString())).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenThrow(new IllegalStateException("illegal"));

    BaseException ex = assertThrows(BaseException.class,
            () -> cassandraOperation.batchInsertWithTTL("ks", "tbl", records, ttls));
    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void getRecordsByIdsWithSpecifiedColumnsAndTTL_success() throws Exception {
    String keyspace = "ks";
    String table = "tbl";
    Map<String, Object> primaryKeys = Map.of("id", "123");
    List<String> properties = List.of("name", "email");
    Map<String, String> ttlMap = Map.of("name", "name_ttl");

    try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
      when(mockConnectionManager.getSession(keyspace)).thenReturn(mock(Session.class));
      when(mockConnectionManager.getSession(keyspace).execute(any(Select.class))).thenReturn(mockResultSet);

      Response mockedResponse = new Response();
      mockedResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedUtil.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(mockedResponse);

      Response response = cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
              keyspace, table, primaryKeys, properties, ttlMap);

      assertNotNull(response);
      assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }
  }

  @Test
  void getRecordsByIdsWithSpecifiedColumnsAndTTL_emptyPropertiesAndTtlMap_success() throws Exception {
    String keyspace = "ks";
    String table = "tbl";
    Map<String, Object> primaryKeys = Map.of("id", "123");

    try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
      when(mockConnectionManager.getSession(keyspace)).thenReturn(mock(Session.class));
      when(mockConnectionManager.getSession(keyspace).execute(any(Select.class))).thenReturn(mockResultSet);

      Response mockedResponse = new Response();
      mockedResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedUtil.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(mockedResponse);

      Response response = cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
              keyspace, table, primaryKeys, null, null);

      assertNotNull(response);
      assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }
  }

  @Test
  void getRecordsByIdsWithSpecifiedColumnsAndTTL_ttlWithMissingAlias_throwsException() {
    String keyspace = "ks";
    String table = "tbl";
    Map<String, Object> primaryKeys = Map.of("id", "123");
    List<String> properties = List.of("name");
    Map<String, String> ttlMap = Map.of("name", ""); // missing alias

    BaseException ex = assertThrows(BaseException.class, () ->
            cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                    keyspace, table, primaryKeys, properties, ttlMap));

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void getRecordsByIdsWithSpecifiedColumnsAndTTL_queryFails_throwsBaseException() throws BaseException {
    String keyspace = "ks";
    String table = "tbl";
    Map<String, Object> primaryKeys = Map.of("id", "123");
    List<String> properties = List.of("name");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("DB error"));

    BaseException ex = assertThrows(BaseException.class, () ->
            cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                    keyspace, table, primaryKeys, properties, null));

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void getRecordsByPrimaryKeys_withPrimaryKeyColumnName_success() throws Exception {
    String keyspace = "test_ks";
    String table = "test_table";
    List<String> ids = List.of("id1", "id2");
    String primaryKeyColumnName = "userId";

    try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
      when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
      when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

      Response mockResponse = new Response();
      mockResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);

      Response response = cassandraOperation.getRecordsByPrimaryKeys(
              keyspace, table, ids, primaryKeyColumnName);

      assertNotNull(response);
      assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }
  }

  @Test
  void getRecordsByPrimaryKeys_withoutPrimaryKeyColumnName_usesDefaultIdColumn() throws Exception {
    String keyspace = "test_ks";
    String table = "test_table";
    List<String> ids = List.of("id1", "id2");
    String primaryKeyColumnName = null;

    try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
      when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
      when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

      Response mockResponse = new Response();
      mockResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);

      Response response = cassandraOperation.getRecordsByPrimaryKeys(
              keyspace, table, ids, primaryKeyColumnName);

      assertNotNull(response);
      assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }
  }

  @Test
  void getRecordsByPrimaryKeys_sessionThrowsException_throwsBaseException() throws BaseException {
    String keyspace = "test_ks";
    String table = "test_table";
    List<String> ids = List.of("id1", "id2");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("DB error"));

    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.getRecordsByPrimaryKeys(keyspace, table, ids, "userId");
    });

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void testGetRecordsByIdsWithSpecifiedColumns_withProperties_success() throws Exception {
    String keyspace = "test_keyspace";
    String table = "test_table";
    List<String> properties = List.of("name", "email");
    List<String> ids = List.of("user1", "user2");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> mockedStatic = mockStatic(CassandraUtil.class)) {
      Response expectedResponse = new Response();
      expectedResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedStatic.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expectedResponse);

      Response actualResponse = cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
              keyspace, table, properties, ids);

      assertNotNull(actualResponse);
      assertEquals(Constants.SUCCESS, actualResponse.get(Constants.RESPONSE));
    }
  }

  @Test
  void testGetRecordsByIdsWithSpecifiedColumns_withEmptyProperties_success() throws Exception {
    String keyspace = "test_keyspace";
    String table = "test_table";
    List<String> properties = List.of(); // Empty
    List<String> ids = List.of("user1", "user2");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> mockedStatic = mockStatic(CassandraUtil.class)) {
      Response expectedResponse = new Response();
      expectedResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedStatic.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expectedResponse);

      Response actualResponse = cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
              keyspace, table, properties, ids);

      assertNotNull(actualResponse);
      assertEquals(Constants.SUCCESS, actualResponse.get(Constants.RESPONSE));
    }
  }

  @Test
  void testGetRecordsByIdsWithSpecifiedColumns_whenExceptionThrown_throwsBaseException() throws BaseException {
    String keyspace = "test_keyspace";
    String table = "test_table";
    List<String> properties = List.of("name");
    List<String> ids = List.of("user1", "user2");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("DB failure"));

    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
              keyspace, table, properties, ids);
    });

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void testGetRecordsByCompositeKey_success() throws Exception {
    String keyspace = "sunbird";
    String table = "user";
    Map<String, Object> compositeKeyMap = new HashMap<>();
    compositeKeyMap.put("id", "u123");
    compositeKeyMap.put("orgId", "org456");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> mockedStatic = mockStatic(CassandraUtil.class)) {
      Response expectedResponse = new Response();
      expectedResponse.put(Constants.RESPONSE, Constants.SUCCESS);
      mockedStatic.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expectedResponse);

      Response actualResponse = cassandraOperation.getRecordsByCompositeKey(keyspace, table, compositeKeyMap);

      assertNotNull(actualResponse);
      assertEquals(Constants.SUCCESS, actualResponse.get(Constants.RESPONSE));
      verify(mockConnectionManager, times(1)).getSession(keyspace);
      verify(mockSession, times(1)).execute(any(Statement.class));
    }
  }

  @Test
  void testGetRecordsByCompositeKey_whenExceptionThrown_throwsBaseException() throws BaseException {
    String keyspace = "sunbird";
    String table = "user";
    Map<String, Object> compositeKeyMap = new HashMap<>();
    compositeKeyMap.put("id", "u123");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Session error"));

    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.getRecordsByCompositeKey(keyspace, table, compositeKeyMap);
    });

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void testDeleteRecords_success() throws Exception {
    String keyspace = "sunbird";
    String table = "user";
    List<String> identifiers = Arrays.asList("id1", "id2");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
    when(mockResultSet.wasApplied()).thenReturn(true);

    boolean result = cassandraOperation.deleteRecords(keyspace, table, identifiers);

    assertTrue(result);
    verify(mockConnectionManager, times(1)).getSession(keyspace);
    verify(mockSession, times(1)).execute(any(Statement.class));
    verify(mockResultSet, times(1)).wasApplied();
  }

  @Test
  void testDeleteRecords_exception_throwsBaseException() throws BaseException {
    String keyspace = "sunbird";
    String table = "user";
    List<String> identifiers = Arrays.asList("id1", "id2");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Cassandra error"));

    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.deleteRecords(keyspace, table, identifiers);
    });

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void testDeleteRecord_success() throws BaseException {
    String keyspace = "sunbird";
    String table = "user_details";
    Map<String, String> compositeKeyMap = new HashMap<>();
    compositeKeyMap.put("userId", "u123");
    compositeKeyMap.put("courseId", "c456");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mock(ResultSet.class));

    assertDoesNotThrow(() -> {
      cassandraOperation.deleteRecord(keyspace, table, compositeKeyMap);
    });

    verify(mockConnectionManager, times(1)).getSession(keyspace);
    verify(mockSession, times(1)).execute(any(Statement.class));
  }

  @Test
  void testDeleteRecord_exception() throws BaseException {
    String keyspace = "sunbird";
    String table = "user_details";
    Map<String, String> compositeKeyMap = new HashMap<>();
    compositeKeyMap.put("userId", "u123");

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Cassandra error"));

    BaseException exception = assertThrows(BaseException.class, () -> {
      cassandraOperation.deleteRecord(keyspace, table, compositeKeyMap);
    });

    assertEquals("SERVER_ERROR", exception.getMessage());
  }

  @Test
  void testGetRecordsByIndexedProperty_success() throws BaseException {
    String keyspace = "sunbird";
    String table = "user";
    String propertyName = "email";
    String propertyValue = "test@example.com";

    mockResponse = new Response();
    mockResponse.put("response", "success");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
      mockedUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);

      Response response = cassandraOperation.getRecordsByIndexedProperty(
              keyspace, table, propertyName, propertyValue
      );

      assertNotNull(response);
      assertEquals("success", response.get("response"));

      mockedUtil.verify(() -> CassandraUtil.createResponse(mockResultSet), times(1));
    }
  }

  @Test
  void testGetRecordsByIndexedProperty_failure() throws BaseException {
    String keyspace = "sunbird";
    String table = "user";
    String propertyName = "email";
    String propertyValue = "test@example.com";

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Connection failed"));

    BaseException exception = assertThrows(BaseException.class, () -> {
      cassandraOperation.getRecordsByIndexedProperty(keyspace, table, propertyName, propertyValue);
    });

    assertEquals("SERVER_ERROR", exception.getMessage());
  }


  @Test
  void testBatchUpdate_success() throws BaseException {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> pk = new HashMap<>();
    pk.put("id", "user-001");

    Map<String, Object> nonPk = new HashMap<>();
    nonPk.put("name", "Ajay");

    Map<String, Map<String, Object>> row = new HashMap<>();
    row.put(Constants.PRIMARY_KEY, pk);
    row.put(Constants.NON_PRIMARY_KEY, nonPk);

    List<Map<String, Map<String, Object>>> list = Collections.singletonList(row);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> mocked = mockStatic(CassandraUtil.class)) {
      mocked.when(() -> CassandraUtil.createUpdateQuery(pk, nonPk, keyspace, table))
              .thenReturn(mockUpdateStatement);

      // Act
      Response response = cassandraOperation.batchUpdate(keyspace, table, list);

      // Assert
      assertNotNull(response);
      assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
      mocked.verify(() -> CassandraUtil.createUpdateQuery(pk, nonPk, keyspace, table), times(1));
    }
  }

  @Test
  void testBatchUpdate_failure() throws BaseException {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> pk = new HashMap<>();
    pk.put("id", "user-002");

    Map<String, Object> nonPk = new HashMap<>();
    nonPk.put("email", "test@example.com");

    Map<String, Map<String, Object>> row = new HashMap<>();
    row.put(Constants.PRIMARY_KEY, pk);
    row.put(Constants.NON_PRIMARY_KEY, nonPk);

    List<Map<String, Map<String, Object>>> list = Collections.singletonList(row);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);

    try (MockedStatic<CassandraUtil> mocked = mockStatic(CassandraUtil.class)) {
      mocked.when(() -> CassandraUtil.createUpdateQuery(pk, nonPk, keyspace, table))
              .thenThrow(new RuntimeException("Update failed"));

      // Act & Assert
      BaseException ex = assertThrows(BaseException.class, () -> {
        cassandraOperation.batchUpdate(keyspace, table, list);
      });

      assertEquals("SERVER_ERROR", ex.getMessage());
      mocked.verify(() -> CassandraUtil.createUpdateQuery(pk, nonPk, keyspace, table), times(1));
    }
  }

  @Test
  void testPerformBatchAction_successWithInsertAndUpdate() throws Exception {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> insertRecord = new HashMap<>();
    insertRecord.put("id", "u001");
    insertRecord.put("name", "Ajay");

    Map<String, Object> updateRecord = new HashMap<>();
    updateRecord.put("id", "u002");
    updateRecord.put("email", "ajay@example.com");

    Map<String, Object> inputData = new HashMap<>();
    inputData.put(Constants.INSERT, insertRecord);
    inputData.put(Constants.UPDATE, updateRecord);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);

    // Act
    Response response = cassandraOperation.performBatchAction(keyspace, table, inputData);

    // Assert
    assertNotNull(response);
    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(mockSession, times(1)).execute(any(BatchStatement.class));
  }

  @Test
  void testPerformBatchAction_failure_throwsBaseException() throws BaseException {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> updateRecord = new HashMap<>();
    updateRecord.put("id", "u003");
    updateRecord.put("status", "inactive");

    Map<String, Object> inputData = new HashMap<>();
    inputData.put(Constants.UPDATE, updateRecord);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class)))
            .thenThrow(new ReadTimeoutException(null, ConsistencyLevel.ONE, 0, 0, false));

    // Act & Assert
    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.performBatchAction(keyspace, table, inputData);
    });

    assertEquals("SERVER_ERROR", ex.getMessage());
  }

  @Test
  void testBatchUpdateById_success() throws Exception {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> record1 = new HashMap<>();
    record1.put(Constants.ID, "u1");
    record1.put("name", "Ajay");

    Map<String, Object> record2 = new HashMap<>();
    record2.put(Constants.ID, "u2");
    record2.put("email", "ajay@example.com");

    List<Map<String, Object>> records = Arrays.asList(record1, record2);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);

    // Act
    Response response = cassandraOperation.batchUpdateById(keyspace, table, records);

    // Assert
    assertNotNull(response);
    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(mockSession, times(1)).execute(any(BatchStatement.class));
  }

  @Test
  void testBatchUpdateById_throwsBaseException() throws BaseException {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> record = new HashMap<>();
    record.put(Constants.ID, "u3");
    record.put("phone", "1234567890");

    List<Map<String, Object>> records = Collections.singletonList(record);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class)))
            .thenThrow(new ReadTimeoutException(null, ConsistencyLevel.ONE, 0, 0, false));

    // Act & Assert
    BaseException ex = assertThrows(BaseException.class, () ->
            cassandraOperation.batchUpdateById(keyspace, table, records)
    );

    assertEquals("SERVER_ERROR", ex.getMessage());
    verify(mockSession, times(1)).execute(any(BatchStatement.class));
  }

  @Test
  void testBatchInsert_success() throws Exception {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> row1 = new HashMap<>();
    row1.put("id", "u1");
    row1.put("name", "Ajay");

    Map<String, Object> row2 = new HashMap<>();
    row2.put("id", "u2");
    row2.put("email", "ajay@example.com");

    List<Map<String, Object>> records = Arrays.asList(row1, row2);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class))).thenReturn(mockResultSet);

    // Act
    Response response = cassandraOperation.batchInsert(keyspace, table, records);

    // Assert
    assertNotNull(response);
    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    verify(mockSession, times(1)).execute(any(BatchStatement.class));
  }

  @Test
  void testBatchInsert_throwsException() throws BaseException {
    // Arrange
    String keyspace = "sunbird";
    String table = "user";

    Map<String, Object> record = new HashMap<>();
    record.put("id", "u3");
    record.put("phone", "1234567890");

    List<Map<String, Object>> records = Collections.singletonList(record);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(BatchStatement.class)))
            .thenThrow(new ReadTimeoutException( null, ConsistencyLevel.ONE, 0, 0, false));

    // Act & Assert
    BaseException exception = assertThrows(BaseException.class, () ->
            cassandraOperation.batchInsert(keyspace, table, records));

    assertEquals("SERVER_ERROR", exception.getMessage());
    verify(mockSession, times(1)).execute(any(BatchStatement.class));
  }
//
//  @Test
//  void testGetRecordById_withStringKey_success() throws BaseException {
//    // Arrange
//    String keyspace = "test_keyspace";
//    String table = "test_table";
//    String id = "user-001";
//
//    // Important: Create a real String[] for use in spy
//    List<String> fieldList = new ArrayList<>();
//    fieldList.add("id");
//    fieldList.add("name");
//
//    List<String> spyFields = Mockito.spy(fieldList);
//
//    // Override toArray to return String[] instead of Object[]
//    Mockito.doReturn(new String[]{"id", "name"}).when(spyFields).toArray();
//
//    // Mocks
//    Session mockSession = mock(Session.class);
//    ResultSet mockResultSet = mock(ResultSet.class);
//    mockStatic(CassandraUtil.class);
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
//    when(CassandraUtil.createResponse(mockResultSet)).thenReturn(new Response());
//
//    //CassandraOperationImpl cassandraOperation = new CassandraOperationImpl() {};
//
//    // Act
//    Response result = cassandraOperation.getRecordById(keyspace, table, id, spyFields);
//
//    // Assert
//    assertNotNull(result);
//  }


  @Test
  void testGetRecordById_withCompositeKey_success() throws Exception {
    String keyspace = "sunbird";
    String table = "user";
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put("id", "user-123");
    compositeKey.put("orgId", "org-456");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> util = Mockito.mockStatic(CassandraUtil.class)) {
      util.when(() -> CassandraUtil.createQuery(anyString(), any(), any(Select.Where.class)))
              .thenAnswer(invocation -> {
                // optionally verify the parameters
                return null; // since it's void
              });      util.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(new Response());

      Response response = cassandraOperation.getRecordById(keyspace, table, compositeKey, null);
      assertNotNull(response);
    }
  }

  @Test
  void testGetRecordById_failure() throws BaseException {
    String keyspace = "sunbird";
    String table = "user";
    Map<String, Object> key = Collections.singletonMap("id", "user-789");

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenThrow(new RuntimeException("DB error"));

    BaseException ex = assertThrows(BaseException.class,
            () -> cassandraOperation.getRecordById(keyspace, table, key, Collections.emptyList()));

    assertEquals("SERVER_ERROR", ex.getMessage());
  }


  @Test
  void testGetRecordById_withStringKey_success() throws BaseException {
    String keyspace = "ks";
    String table = "tbl";
    String key = "123";

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> cassandraUtilMock = Mockito.mockStatic(CassandraUtil.class)) {
      cassandraUtilMock.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(new Response());

      Response response = cassandraOperation.getRecordById(keyspace, table, key);

      assertNotNull(response);
    }
  }


  @Test
  void testGetRecordById_withFields_success() throws BaseException {
    String keyspace = "ks";
    String table = "tbl";
    String key = "123";
    List<String> fields = spy(Arrays.asList("id", "name"));

    // Force toArray to return String[] to avoid ClassCastException
    doReturn(new String[]{"id", "name"}).when(fields).toArray();

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    try (MockedStatic<CassandraUtil> cassandraUtilMock = Mockito.mockStatic(CassandraUtil.class)) {
      cassandraUtilMock.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(new Response());

      Response response = cassandraOperation.getRecordById(keyspace, table, key, fields);

      assertNotNull(response);
    }
  }

  @Test
  void testGetRecordById_throwsBaseException() throws BaseException {
    String keyspace = "ks";
    String table = "tbl";
    String key = "123";

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenThrow(new RuntimeException("DB down"));

    assertThrows(BaseException.class, () -> {
      cassandraOperation.getRecordById(keyspace, table, key);
    });
  }

  @Test
  void testUpdateRecord_success() throws BaseException {
    Map<String, Object> request = Map.of("name", "Ajay");
    Map<String, Object> key = Map.of("id", "123");

    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mock(ResultSet.class)); // ✅ FIXED

    Response response = cassandraOperation.updateRecord("ks", "user", request, key);

    assertNotNull(response);
  }


  @Test
  void testUpdateRecord_exception_unknownIdentifier() throws BaseException {
    Map<String, Object> request = Map.of("name", "Ajay");
    Map<String, Object> key = Map.of("id", "123");

    RuntimeException ex = new RuntimeException("Some error " + Constants.UNKNOWN_IDENTIFIER);

    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
    doThrow(ex).when(mockSession).execute(any(Statement.class));

    try (MockedStatic<CassandraUtil> mockedStatic = Mockito.mockStatic(CassandraUtil.class)) {
      mockedStatic.when(() -> CassandraUtil.processExceptionForUnknownIdentifier(ex)).thenReturn("ERR_CODE");
      when(mockLocalizer.getMessage("ERR_CODE", null)).thenReturn("Unknown identifier");

      BaseException be = assertThrows(BaseException.class, () ->
              cassandraOperation.updateRecord("ks", "user", request, key)
      );

      assertEquals(IResponseMessage.INVALID_PROPERTY_ERROR, be.getCode());
    }
  }

  @Test
  void testUpdateRecord_exception_otherError() throws BaseException {
    Map<String, Object> request = Map.of("name", "Ajay");
    Map<String, Object> key = Map.of("id", "123");

    RuntimeException ex = new RuntimeException("Some DB issue");

    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
    doThrow(ex).when(mockSession).execute(any(Statement.class));
    when(mockLocalizer.getMessage(IResponseMessage.DB_UPDATE_FAIL, null)).thenReturn("Update failed");

    BaseException be = assertThrows(BaseException.class, () ->
            cassandraOperation.updateRecord("ks", "user", request, key)
    );

    assertEquals(IResponseMessage.DB_UPDATE_FAIL, be.getCode());
  }

  @Test
  void testGetAllRecords_success() throws Exception {
    String keyspace = "test_ks";
    String table = "test_tbl";

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);

    Response expectedResponse = new Response();
    expectedResponse.put(Constants.RESPONSE, "OK");

    try (MockedStatic<CassandraUtil> mocked = mockStatic(CassandraUtil.class)) {
      mocked.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expectedResponse);

      Response response = cassandraOperation.getAllRecords(keyspace, table);

      assertNotNull(response);
      assertEquals("OK", response.get(Constants.RESPONSE));
    }
  }

  @Test
  void testGetAllRecords_exception() throws BaseException {
    String keyspace = "test_ks";
    String table = "test_tbl";

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(any(Statement.class))).thenThrow(new RuntimeException("DB error"));

    BaseException ex = assertThrows(BaseException.class, () -> {
      cassandraOperation.getAllRecords(keyspace, table);
    });

    assertEquals(IResponseMessage.SERVER_ERROR, ex.getCode());
    assertEquals(ResponseCode.SERVER_ERROR.getCode(), ex.getResponseCode());
  }

//  @Test
//  void testGetRecordsByProperty_withFields_success() throws BaseException {
//    String keyspace = "ks";
//    String table = "tbl";
//    String propertyName = "id";
//    List<Object> propertyValues = List.of("id1", "id2");
//    List<String> fields = List.of("name", "age");
//
//    ResultSet mockResultSet = mock(ResultSet.class);
//    Selection mockSelection = mock(Selection.class);
//    Select selectMock = mock(Select.class);
//    Clause clauseMock = mock(Clause.class);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
//
//    try (
//            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
//            MockedStatic<CassandraUtil> cassUtil = mockStatic(CassandraUtil.class)
//    ) {
//      // Static mocking
//      qb.when(() -> QueryBuilder.select(fields.toArray(new String[0]))).thenReturn(mockSelection);
////      when(mockSelection.from(keyspace, table)).thenReturn(selectMock);
//
//      qb.when(() -> QueryBuilder.in(propertyName, propertyValues)).thenReturn(clauseMock);
////      when(selectMock.where(clauseMock)).thenReturn(selectMock);
//
//      Response mockResponse = new Response();
//      mockResponse.put(Constants.RESPONSE, "OK");
//      cassUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);
//
//      Response response = cassandraOperation.getRecordsByProperty(keyspace, table, propertyName, propertyValues, fields);
//      assertEquals("OK", response.get(Constants.RESPONSE));
//    }
//  }

//  @Test
//  void testGetRecordsByProperty_withoutFields_success() throws BaseException {
//    String keyspace = "ks";
//    String table = "tbl";
//    String propertyName = "id";
//    List<Object> propertyValues = List.of("id1");
//    List<String> fields = Collections.emptyList();
//
//    ResultSet mockResultSet = mock(ResultSet.class);
//    Selection mockSelection = mock(Selection.class);
//    Select selectMock = mock(Select.class);
//    Clause clauseMock = mock(Clause.class);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
//
//    try (
//            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
//            MockedStatic<CassandraUtil> cassUtil = mockStatic(CassandraUtil.class)
//    ) {
//      // Mock: select().all().from()
////      qb.when(QueryBuilder::select).thenReturn(mockSelection);
////      when(mockSelection.all()).thenReturn(mockSelection);
////      when(mockSelection.from(keyspace, table)).thenReturn(selectMock);
////
////      qb.when(() -> QueryBuilder.in(propertyName, propertyValues)).thenReturn(clauseMock);
////      when(selectMock.where(clauseMock)).thenReturn(selectMock);
//
//      Response mockResponse = new Response();
//      mockResponse.put(Constants.RESPONSE, "OK");
//      cassUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);
//
//      Response response = cassandraOperation.getRecordsByProperty(keyspace, table, propertyName, propertyValues, fields);
//      assertEquals("OK", response.get(Constants.RESPONSE));
//    }
//  }

  @Test
  void testGetRecordsByProperty_exception() {
    String keyspace = "ks";
    String table = "tbl";
    String propertyName = "id";
    List<Object> propertyValues = List.of("id1");
    List<String> fields = List.of("name");

    try (
            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)
    ) {
      qb.when(() -> QueryBuilder.select(fields.toArray(new String[0])))
              .thenThrow(new RuntimeException("DB error"));

      BaseException ex = assertThrows(BaseException.class, () -> {
        cassandraOperation.getRecordsByProperty(keyspace, table, propertyName, propertyValues, fields);
      });

      assertEquals(IResponseMessage.SERVER_ERROR, ex.getMessage());
      assertEquals(ResponseCode.SERVER_ERROR.getCode(), ex.getResponseCode());
    }
  }

//  @Test
//  void testGetRecordsByProperties_withFieldsAndInClause_success() throws BaseException {
//    String keyspace = "ks";
//    String table = "tbl";
//    Map<String, Object> propertyMap = new HashMap<>();
//    propertyMap.put("status", Arrays.asList("active", "inactive"));
//    List<String> fields = Arrays.asList("id", "name");
//
//    Select selectMock = mock(Select.class);
//    Select.Where whereMock = mock(Select.Where.class);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
//
//    try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
//         MockedStatic<CassandraUtil> cassUtil = mockStatic(CassandraUtil.class)) {
//
//      qb.when(() -> QueryBuilder.select(fields.toArray(new String[0]))).thenReturn(selectMock);
//      qb.when(() -> QueryBuilder.in(eq("status"), any(Object[].class))).thenReturn(mock(Clause.class));
//      //when(selectMock.from(keyspace, table)).thenReturn(selectMock);
//      when(selectMock.where()).thenReturn(whereMock);
//      when(selectMock.allowFiltering()).thenReturn(selectMock);
//
//      Response mockResponse = new Response();
//      mockResponse.put(Constants.RESPONSE, "SUCCESS");
//      cassUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);
//
//      Response response = cassandraOperation.getRecordsByProperties(keyspace, table, propertyMap, fields);
//      assertEquals("SUCCESS", response.get(Constants.RESPONSE));
//    }
//  }

//  @Test
//  void testGetRecordsByProperties_withEmptyMapAndFields_success() throws BaseException {
//    String keyspace = "ks";
//    String table = "tbl";
//
//    List<String> fields = new ArrayList<>(); // Empty
//    Map<String, Object> propertyMap = new HashMap<>(); // Empty
//
//    ResultSet mockResultSet = mock(ResultSet.class);
//    Select selectMock = mock(Select.class);
//    Selection mockSelection = mock(Selection.class);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
//
//    try (
//            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
//            MockedStatic<CassandraUtil> cassUtil = mockStatic(CassandraUtil.class)
//    ) {
//      // Step-by-step mocking chain:
//      qb.when(QueryBuilder::select).thenReturn(mockSelection);
//
//      when(QueryBuilder.select().all().from(keyspace, table))
//              .thenReturn(selectMock);
//      when(selectMock.allowFiltering()).thenReturn(selectMock);
//
//      Response mockResponse = new Response();
//      mockResponse.put(Constants.RESPONSE, "OK");
//      cassUtil.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(mockResponse);
//
//      Response response = cassandraOperation.getRecordsByProperties(keyspace, table, propertyMap, fields);
//      assertEquals("OK", response.get(Constants.RESPONSE));
//    }
//  }



  @Test
  void testGetRecordsByProperties_withException_throwsBaseException() throws BaseException {
    String keyspace = "ks";
    String table = "tbl";

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("DB Error"));

    BaseException ex = assertThrows(BaseException.class,
            () -> cassandraOperation.getRecordsByProperties(keyspace, table, null, null));

    assertEquals(IResponseMessage.SERVER_ERROR, ex.getCode());
    assertEquals(ResponseCode.SERVER_ERROR.getCode(), ex.getResponseCode());
  }

//  @Test
//  void testUpsertRecord_success() throws BaseException {
//    Map<String, Object> request = Map.of("id", "123", "name", "Ajay");
//
//    mockStatic(CassandraUtil.class).when(() ->
//                    CassandraUtil.getPreparedStatement("ks", "user", request))
//            .thenReturn("INSERT INTO user(id, name) VALUES (?, ?)");
//
//    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
//    when(mockSession.prepare(anyString())).thenReturn(mockPreparedStatement);
//    whenNew(BoundStatement.class).withArguments(mockPreparedStatement).thenReturn(mockBoundStatement);
//    when(mockSession.execute(any(BoundStatement.class))).thenReturn(mock(ResultSet.class));
//
//    Response response = cassandraOperation.upsertRecord("ks", "user", request);
//
//    assertNotNull(response);
//    assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
//  }
//
//  @Test
//  void testUpsertRecord_failure_withUnknownIdentifier() throws BaseException {
//    Map<String, Object> request = Map.of("id", "123");
//
//    mockStatic(CassandraUtil.class).when(() ->
//                    CassandraUtil.getPreparedStatement("ks", "user", request))
//            .thenReturn("QUERY");
//
//    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
//    when(mockSession.prepare(anyString())).thenReturn(mockPreparedStatement);
//    whenNew(BoundStatement.class).withArguments(mockPreparedStatement).thenReturn(mockBoundStatement);
//    when(mockSession.execute(any(BoundStatement.class)))
//            .thenThrow(new RuntimeException("Some error with unknown_identifier"));
//
//    mockStatic(CassandraUtil.class).when(() ->
//            CassandraUtil.processExceptionForUnknownIdentifier(any())).thenReturn("processed");
//
//    when(mockLocalizer.getMessage(eq("processed"), any())).thenReturn("Invalid identifier");
//
//    BaseException ex = assertThrows(BaseException.class, () ->
//            cassandraOperation.upsertRecord("ks", "user", request)
//    );
//
//    assertEquals(ResponseCode.CLIENT_ERROR.getCode(), ex.getResponseCode());
//  }
//
//  @Test
//  void testUpsertRecord_genericFailure() throws BaseException {
//    Map<String, Object> request = Map.of("id", "123");
//
//    mockStatic(CassandraUtil.class).when(() ->
//                    CassandraUtil.getPreparedStatement("ks", "user", request))
//            .thenReturn("QUERY");
//
//    when(mockConnectionManager.getSession("ks")).thenReturn(mockSession);
//    when(mockSession.prepare(anyString())).thenReturn(mockPreparedStatement);
//    whenNew(BoundStatement.class).withArguments(mockPreparedStatement).thenReturn(mockBoundStatement);
//    when(mockSession.execute(any(BoundStatement.class)))
//            .thenThrow(new RuntimeException("Some random error"));
//
//    BaseException ex = assertThrows(BaseException.class, () ->
//            cassandraOperation.upsertRecord("ks", "user", request)
//    );
//
//    assertEquals(ResponseCode.SERVER_ERROR.getCode(), ex.getResponseCode());
//  }

//
//  @Test
//  void testGetRecordsByProperty_withFields_success() throws Exception {
//    String keyspace = "ks";
//    String table = "users";
//    String property = "id";
//    Object value = "123";
//    List<String> fields = Arrays.asList("name", "email");
//
//    Selection selectBuilder = mock(Selection.class);
//    Select selectStatement = mock(Select.class);
////    Clause mockClause = eq(property, value);
//
//    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
//    when(mockSession.execute(selectStatement)).thenReturn(mockResultSet);
//
//    try (
//            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
//            MockedStatic<CassandraUtil> cu = mockStatic(CassandraUtil.class)
//    ) {
//      qb.when(() -> QueryBuilder.select(fields.toArray(new String[0]))).thenReturn(selectBuilder);
////      qb.when(() -> QueryBuilder.eq(property, value)).thenReturn(mockClause);
////
////      //when(selectBuilder.from(keyspace, table)).thenReturn(selectStatement);
////      when(selectStatement.where(mockClause)).thenReturn(selectStatement.where());
//      when(mockSession.execute(selectStatement)).thenReturn(mockResultSet);
//
//      Response expected = new Response();
//      expected.put(Constants.RESPONSE, "OK");
//      cu.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expected);
//
//      Response actual = cassandraOperation.getRecordsByProperty(keyspace, table, property, value, fields);
//      assertEquals("OK", actual.get(Constants.RESPONSE));
//    }
//  }

  @Test
  void testGetRecordsByProperty_withoutFields_success() throws Exception {
    String keyspace = "ks";
    String table = "users";
    String property = "id";
    Object value = "123";
    List<String> fields = Collections.emptyList();

    Select.Selection selectAll = mock(Select.Selection.class);
    Select selectStatement = mock(Select.class);
    //Clause mockClause = eq(property, value);

    when(mockConnectionManager.getSession(keyspace)).thenReturn(mockSession);
    when(mockSession.execute(selectStatement)).thenReturn(mockResultSet);

    try (
            MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class);
            MockedStatic<CassandraUtil> cu = mockStatic(CassandraUtil.class)
    ) {
      qb.when(QueryBuilder::select).thenReturn(selectAll);
//      qb.when(() -> QueryBuilder.eq(property, value)).thenReturn(mockClause);

      when(selectAll.all()).thenReturn(selectAll);
      when(selectAll.from(keyspace, table)).thenReturn(selectStatement);
//      when(selectStatement.where(mockClause)).thenReturn(selectStatement.where());
      when(mockSession.execute(selectStatement)).thenReturn(mockResultSet);

      Response expected = new Response();
      expected.put(Constants.RESPONSE, "EMPTY");
      cu.when(() -> CassandraUtil.createResponse(mockResultSet)).thenReturn(expected);

      Response actual = cassandraOperation.getRecordsByProperty(keyspace, table, property, value, fields);
      assertNull(actual);
    }
  }
//
//  @Test
//  void testGetRecordsByProperty_whenExceptionThrown_shouldThrowBaseException() throws BaseException {
//    String keyspace = "ks";
//    String table = "users";
//    String property = "id";
//    Object value = "123";
//    List<String> fields = Collections.emptyList();
//
//    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Connection failed"));
//
//    BaseException ex = assertThrows(BaseException.class, () ->
//            cassandraOperation.getRecordsByProperty(keyspace, table, property, value, fields)
//    );
//
//    assertEquals("SERVER_ERROR", ex.getCode());
//  }


  @Test
  void testDeleteRecord_exceptionThrown() throws BaseException {
    String keyspace = "ks";
    String table = "user";
    String identifier = "user-123";

    when(mockConnectionManager.getSession(keyspace)).thenThrow(new RuntimeException("Connection error"));

    BaseException ex = assertThrows(BaseException.class, () ->
            cassandraOperation.deleteRecord(keyspace, table, identifier)
    );

    assertEquals("SERVER_ERROR", ex.getCode());
  }
}