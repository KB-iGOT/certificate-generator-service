
package org.sunbird.cassandraimpl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.helper.*;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;

import java.text.MessageFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
  Cluster.class,
  CassandraOperationImpl.class,
  Uninterruptibles.class,
  PreparedStatement.class,
  BoundStatement.class,
  Session.class,
  Metadata.class,
  CassandraConnectionMngrFactory.class,
  ResultSet.class,
  CassandraUtil.class,
  Cluster.Builder.class,
  Select.class,
  Row.class,
  ColumnDefinitions.class,
  String.class,
  Select.Where.class,
  Select.Builder.class,
  QueryBuilder.class,
  Select.Selection.class,
  Delete.Where.class,
  Delete.Selection.class,
  CassandraConnectionManager.class,
  CassandraConnectionManagerImpl.class,
  CassandraConnectionMngrFactory.class,
  PoolingOptions.class,
  PropertiesCache.class,
  CassandraUtil.class,
  PreparedStatement.class,
  Session.class

})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class CassandraOperationImplTest {
  Localizer localizer = Localizer.getInstance();
  private static Cluster cluster;
  private static Session session = PowerMockito.mock(Session.class);
  private static PreparedStatement statement;
  private static ResultSet resultSet;
  private static Select selectQuery;
  private static Select.Where where;
  private static Delete.Where deleteWhere;
  private static Select.Builder selectBuilder;
  private static Metadata metadata;
  private static CassandraOperation operation;
  private static Map<String, Object> address = null;
  private static Map<String, Object> dummyAddress = null;
  private static PropertiesCache propertiesCache = null;
  private static final Cluster.Builder builder = PowerMockito.mock(Cluster.Builder.class);
  private static BoundStatement boundStatement;
  private static Select.Selection selectSelection;
  private static Delete.Selection deleteSelection;
  private static Delete delete;
  private static KeyspaceMetadata keyspaceMetadata;
  private static CassandraConnectionManagerImpl connectionManager= null;
  private static PreparedStatement preparedStatement = null;


  @BeforeClass
  public static void init() throws Exception {

    /*PowerMockito.mockStatic(Cluster.class);
    cluster = PowerMockito.mock(Cluster.class);
    when(cluster.connect(Mockito.anyString())).thenReturn(session);
    metadata = PowerMockito.mock(Metadata.class);
    when(cluster.getMetadata()).thenReturn(metadata);
    when(Cluster.builder()).thenReturn(builder);
    when(builder.addContactPoint(Mockito.anyString())).thenReturn(builder);
    when(builder.withPort(Mockito.anyInt())).thenReturn(builder);
    when(builder.withProtocolVersion(Mockito.any())).thenReturn(builder);
    when(builder.withRetryPolicy(Mockito.any())).thenReturn(builder);
    when(builder.withTimestampGenerator(Mockito.any())).thenReturn(builder);
    when(builder.withPoolingOptions(Mockito.any())).thenReturn(builder);
    when(builder.build()).thenReturn(cluster);
    PowerMockito.mockStatic(CassandraConnectionMngrFactory.class);
    PowerMockito.mock(CassandraConnectionManager.class);
    connectionManager = PowerMockito.mock(CassandraConnectionManagerImpl.class);
    PowerMockito.whenNew(CassandraConnectionManagerImpl.class).withArguments(Constants.STANDALONE_MODE).thenReturn(connectionManager);
    PowerMockito.mock(PoolingOptions.class);
    propertiesCache = PowerMockito.mock(PropertiesCache.class);
    PowerMockito.whenNew(PropertiesCache.class).withNoArguments().thenReturn(propertiesCache);
    PowerMockito.when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(connectionManager.createConnection(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(true);
    PowerMockito.mockStatic(CassandraUtil.class);
    PowerMockito.when(CassandraUtil.getPreparedStatement(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap())).thenReturn(Mockito.anyString());
    preparedStatement = PowerMockito.mock(PreparedStatement.class);
    PowerMockito.when(connectionManager.getSession(Mockito.anyString())).thenReturn(session);
    PowerMockito.when(session.prepare(Mockito.anyString())).thenReturn(preparedStatement);
    PowerMockito.whenNew(BoundStatement.class).withArguments(Mockito.any(PreparedStatement.class)).thenReturn(boundStatement);
*/
  }

  @Before
  public void setUp() throws Exception {

    /*reset(session);
    address = new HashMap<>();

    statement = PowerMockito.mock(PreparedStatement.class);
    selectQuery = PowerMockito.mock(Select.class);
    where = PowerMockito.mock(Select.Where.class);
    selectBuilder = PowerMockito.mock(Select.Builder.class);
    PowerMockito.mockStatic(QueryBuilder.class);
    selectSelection = PowerMockito.mock(Select.Selection.class);
    deleteSelection = PowerMockito.mock(Delete.Selection.class);
    deleteWhere = PowerMockito.mock(Delete.Where.class);
    delete = PowerMockito.mock(Delete.class);
    operation = ServiceFactory.getInstance();
    resultSet = PowerMockito.mock(ResultSet.class);
    keyspaceMetadata = PowerMockito.mock(KeyspaceMetadata.class);
    when(QueryBuilder.select()).thenReturn(selectSelection);
    when(deleteSelection.from(Mockito.anyString(), Mockito.anyString())).thenReturn(delete);
    when(delete.where(QueryBuilder.eq(Constants.IDENTIFIER, "123"))).thenReturn(deleteWhere);
    when(selectQuery.where()).thenReturn(where);
    when(metadata.getKeyspace("sunbird")).thenReturn(keyspaceMetadata);
    when(cluster.connect(Mockito.anyString())).thenReturn(session);
    boundStatement = PowerMockito.mock(BoundStatement.class);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);
    when(session.prepare(Mockito.anyString())).thenReturn(statement);
    when(selectSelection.all()).thenReturn(selectBuilder);
    when(selectBuilder.from(Mockito.anyString(), Mockito.anyString())).thenReturn(selectQuery);
    when(session.execute(selectQuery)).thenReturn(resultSet);

    ColumnDefinitions cd = PowerMockito.mock(ColumnDefinitions.class);
    String str = "qwertypower(king";
    when(resultSet.getColumnDefinitions()).thenReturn(cd);
    when(cd.toString()).thenReturn(str);
    when(str.substring(8, resultSet.getColumnDefinitions().toString().length() - 1))
        .thenReturn(str);*/
  }

  //@Test
  public void testInsertRecordSuccess() throws Exception {

    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Response response = operation.insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));
    assertEquals(ResponseCode.OK.getCode(), response.get("response"));
  }

  //@Test
  public void testInsertRecordFailure() throws Exception {

    when(session.execute(boundStatement.bind("123")))
        .thenThrow(
            new BaseException(
                IResponseMessage.DB_INSERTION_FAIL,
                localizer.getMessage(IResponseMessage.DB_INSERTION_FAIL,null),
                ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));
    } catch (Exception ex) {
      exception = ex;
    }
    assertEquals(localizer.getMessage(IResponseMessage.DB_INSERTION_FAIL,null), exception.getMessage());
  }

  //@Test
  public void testInsertRecordFailureWithInvalidProperty() throws Exception {

    when(session.execute(boundStatement.bind("123")))
        .thenThrow(
            new BaseException(
                IResponseMessage.INVALID_PROPERTY_ERROR,
                    localizer.getMessage(MessageFormat.format(localizer.getMessage(IResponseMessage.INVALID_PROPERTY_ERROR,null
                    ),Constants.UNKNOWN_IDENTIFIER),null),
                ResponseCode.CLIENT_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));
    } catch (Exception exp) {
      exception = exp;
    }
    Object[] args = {""};
    assertEquals(
        new MessageFormat(IResponseMessage.INVALID_PROPERTY_ERROR).format(args),
        exception.getMessage());
  }

  //@Test
  public void testUpdateRecordSuccess() throws BaseException {
    when(session.execute(boundStatement)).thenReturn(resultSet);
    Response response = operation.updateRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));
    assertEquals(ResponseCode.OK, response.get("response"));
  }

  //@Test
  public void testUpdateRecordFailure() throws BaseException {

    when(session.prepare(Mockito.anyString()))
        .thenThrow(
            new BaseException(
                IResponseMessage.DB_UPDATE_FAIL,
                localizer.getMessage(IResponseMessage.DB_UPDATE_FAIL,null),
                ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.updateRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));

    } catch (Exception ex) {
      exception = ex;
    }
    assertEquals(localizer.getMessage(IResponseMessage.DB_UPDATE_FAIL,null), exception.getMessage());
  }

  //@Test
  public void testUpdateRecordFailureWithInvalidProperty() throws Exception {

    when(session.prepare(Mockito.anyString()))
        .thenThrow(
                new BaseException(
                        IResponseMessage.INVALID_PROPERTY_ERROR,
                        localizer.getMessage(MessageFormat.format(IResponseMessage.INVALID_PROPERTY_ERROR,Constants.UNKNOWN_IDENTIFIER),null),
                        ResponseCode.CLIENT_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.updateRecord(Mockito.anyString(), Mockito.anyString(), Mockito.any(Map.class));
    } catch (Exception exp) {
      exception = exp;
    }
    Object[] args = {""};
    assertEquals(
        new MessageFormat(localizer.getMessage(IResponseMessage.INVALID_PROPERTY_ERROR,null)).format(args),
        exception.getMessage());
  }

  //@Test
  public void testGetAllRecordsSuccess() throws Exception {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);

    Response response = operation.getAllRecords(Mockito.anyString(), Mockito.anyString());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetAllRecordsFailure() throws Exception {

    when(session.execute(selectQuery))
        .thenThrow(
            new BaseException(
                    IResponseMessage.SERVER_ERROR,
                    IResponseMessage.SERVER_ERROR,
                ResponseCode.SERVER_ERROR.getCode()));

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);

    Throwable exception = null;
    try {
      operation.getAllRecords(Mockito.anyString(), Mockito.anyString());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testGetPropertiesValueSuccessById() throws Exception {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenReturn(boundStatement);
    Response response =
        operation.getPropertiesValueById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetPropertiesValueFailureById() throws Exception {

    Throwable exception = null;
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenThrow(
            new BaseException(
                    IResponseMessage.SERVER_ERROR,
                    IResponseMessage.SERVER_ERROR,
                ResponseCode.SERVER_ERROR.getCode()));

    try {
      operation.getPropertiesValueById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

 // @Test
  public void testGetRecordSuccessById() throws BaseException {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    when(session.execute(where)).thenReturn(resultSet);
    when(selectBuilder.from(Mockito.anyString(), Mockito.anyString())).thenReturn(selectQuery);
    when(selectSelection.all()).thenReturn(selectBuilder);

    Response response = operation.getRecordById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetRecordFailureById() throws Exception {

    Throwable exception = null;
    PowerMockito.whenNew(BoundStatement.class)
        .withArguments(Mockito.any(PreparedStatement.class))
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    try {
      operation.getRecordById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testGetRecordSuccessByProperties() throws Exception {

    Map<String, Object> map = new HashMap<>();

    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);

    Response response = operation.getRecordsByProperties(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetRecordFailureByProperties() throws Exception {

    Map<String, Object> map = new HashMap<>();

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperties(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testGetRecordForListSuccessByProperties() throws Exception {

    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");

    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    Response response =
        operation.getRecordsByProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList());
    assertTrue(response.getResult().size() > 0);
  }

 // @Test
  public void testGetRecordForListFailreByProperties() throws Exception {

    List<Object> list = new ArrayList<>();
    list.add("123");
    list.add("321");

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testGetRecordsSuccessByProperty() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(boundStatement.bind("123"))).thenReturn(resultSet);
    Response response =
        operation.getRecordsByProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetRecordsFailureByProperty() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.getRecordsByProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testGetRecordsSuccessById() throws BaseException {
    Iterator<Row> rowItr = Mockito.mock(Iterator.class);
    Mockito.when(resultSet.iterator()).thenReturn(rowItr);
    when(session.execute(where)).thenReturn(resultSet);
    when(selectSelection.all()).thenReturn(selectBuilder);

    Response response = operation.getRecordById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(response.getResult().size() > 0);
  }

  //@Test
  public void testGetRecordsFailureById() throws Exception {

    List<Row> rows = new ArrayList<>();
    Row row = Mockito.mock(Row.class);
    rows.add(row);
    when(resultSet.all()).thenReturn(rows);

    when(selectSelection.all())
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.getRecordById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

  //@Test
  public void testDeleteRecordSuccess() throws Exception {

    when(QueryBuilder.delete()).thenReturn(deleteSelection);
    Response response = new Response();
    response.put(JsonKeys.RESPONSE, Constants.SUCCESS);
    operation.deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertEquals("SUCCESS", response.get("response"));
  }

 // @Test
  public void testDeleteRecordFailure() {

    when(QueryBuilder.delete())
        .thenThrow(
                new BaseException(
                        IResponseMessage.SERVER_ERROR,
                        IResponseMessage.SERVER_ERROR,
                        ResponseCode.SERVER_ERROR.getCode()));

    Throwable exception = null;
    try {
      operation.deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue(
        (((BaseException) exception).getResponseCode())
            == ResponseCode.SERVER_ERROR.getCode());
  }

 // @Test
  public void testGetTableListSuccess() throws Exception {

    Collection<TableMetadata> tables = new ArrayList<>();
    TableMetadata table = Mockito.mock(TableMetadata.class);
    tables.add(table);
    when(keyspaceMetadata.getTables()).thenReturn(tables);

    List<String> tableList = connectionManager.getTableList(Mockito.anyString());
    assertTrue(tableList.size() > 0);
  }

  //@Test
  public void testGetClusterSuccess() throws Exception {

    Cluster cluster = connectionManager.getCluster(Mockito.anyString());
    assertTrue(cluster != null);
  }

  //@Test
  public void testGetClusterFailureWithInvalidKeySpace() {

    Throwable exception = null;
    try {
      connectionManager.getCluster(Mockito.anyString());
    } catch (Exception ex) {
      exception = ex;
    }
    assertTrue("cassandra cluster value is null for this sun".equals(exception.getMessage()));
  }
}

