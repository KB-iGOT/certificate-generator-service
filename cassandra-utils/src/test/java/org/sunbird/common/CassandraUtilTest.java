package org.sunbird.common;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.sunbird.cassandraannotation.ClusteringKey;
import org.sunbird.cassandraannotation.PartitioningKey;
import org.sunbird.helper.CassandraPropertyReader;
import org.sunbird.response.Response;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CassandraUtilTest {


    static class BadModel {
        @PartitioningKey
        private String id = "bad";
    }


    @Test
    void getPreparedStatementReturnsValidInsertQuery() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1);
        map.put("name", "foo");
        String query = CassandraUtil.getPreparedStatement("ks", "tbl", map);
        assertTrue(query.contains("INSERT INTO"));
        assertTrue(query.contains("id,name"));
        assertTrue(query.contains("?,?"));
    }

    @Test
    void createResponseReturnsResponseWithMappedRows() {
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);
        List<Row> rows = List.of(row, row);
        Iterator<Row> iterator = rows.iterator();
        when(resultSet.iterator()).thenReturn(iterator);
        ColumnDefinitions defs = mock(ColumnDefinitions.class);
        ColumnDefinitions.Definition def1 = mock(ColumnDefinitions.Definition.class);
        when(def1.getName()).thenReturn("col1");
        when(defs.asList()).thenReturn(List.of(def1));
        when(resultSet.getColumnDefinitions()).thenReturn(defs);

        try (MockedStatic<CassandraPropertyReader> cacheMock = mockStatic(CassandraPropertyReader.class)) {
            CassandraPropertyReader reader = mock(CassandraPropertyReader.class);
            cacheMock.when(CassandraPropertyReader::getInstance).thenReturn(reader);
            when(reader.readProperty("col1")).thenReturn("col1");
            when(row.getObject("col1")).thenReturn("val");
            Response resp = CassandraUtil.createResponse(resultSet);
            assertTrue(resp.containsKey("response"));
            List<?> list = (List<?>) resp.get("response");
            assertEquals(2, list.size());
        }
    }

    @Test
    void fetchColumnsMappingReturnsMappedColumns() {
        ResultSet resultSet = mock(ResultSet.class);
        ColumnDefinitions defs = mock(ColumnDefinitions.class);
        ColumnDefinitions.Definition def1 = mock(ColumnDefinitions.Definition.class);
        when(def1.getName()).thenReturn("col1");
        when(defs.asList()).thenReturn(List.of(def1));
        when(resultSet.getColumnDefinitions()).thenReturn(defs);

        try (MockedStatic<CassandraPropertyReader> cacheMock = mockStatic(CassandraPropertyReader.class)) {
            CassandraPropertyReader reader = mock(CassandraPropertyReader.class);
            cacheMock.when(CassandraPropertyReader::getInstance).thenReturn(reader);
            when(reader.readProperty("col1")).thenReturn("col1 ");
            Map<String, String> map = CassandraUtil.fetchColumnsMapping(resultSet);
            assertEquals("col1", map.keySet().iterator().next().trim());
            assertEquals("col1", map.values().iterator().next().trim());
        }
    }

    @Test
    void getUpdateQueryStatementReturnsValidUpdateQuery() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1);
        map.put("name", "foo");
        String query = CassandraUtil.getUpdateQueryStatement("ks", "tbl", map);
        assertTrue(query.contains("UPDATE"));
        assertTrue(query.contains("SET"));
    }

    @Test
    void getSelectStatementReturnsValidSelectQuery() {
        String query = CassandraUtil.getSelectStatement("ks", "tbl", "id", "name");
        assertTrue(query.contains("SELECT"));
        assertTrue(query.contains("id,name"));
        assertTrue(query.contains("FROM"));
    }

    @Test
    void processExceptionForUnknownIdentifierFormatsMessage() {
        Exception e = new Exception("unknown identifier: foo");
        String msg = CassandraUtil.processExceptionForUnknownIdentifier(e);
        assertTrue(msg.contains("INVALID_PROPERTY_ERROR"));
    }

    static class TestModel {
        @PartitioningKey
        private String id = "id1";
        @ClusteringKey
        private String cluster = "c1";
        private String name = "foo";
    }

    @Test
    void batchUpdateQueryReturnsPrimaryAndNonPrimaryKeyMaps() throws Exception {
        TestModel model = new TestModel();
        Map<String, Map<String, Object>> map = CassandraUtil.batchUpdateQuery(model);
        assertNotNull(map);
    }

    @Test
    void getPrimaryKeyReturnsPrimaryKeyMap() throws Exception {
        TestModel model = new TestModel();
        Map<String, Object> map = CassandraUtil.getPrimaryKey(model);
        assertEquals("id1", map.get("id"));
        assertEquals("c1", map.get("cluster"));
        assertFalse(map.containsKey("name"));
    }

    @Test
    void createWhereQueryHandlesMapWithOperators() {
        Select.Where where = mock(Select.Where.class);
        when(where.and(any())).thenReturn(where);
        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.lte("k", 5)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.lt("k", 4)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.gte("k", 2)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.gt("k", 1)).thenReturn(mock(Clause.class));
            CassandraUtil.createWhereQuery("k", Map.of("lte", 5, "lt", 4, "gte", 2, "gt", 1), where);
        }
    }

    @Test
    void createWhereQueryHandlesListValue() {
        Select.Where where = mock(Select.Where.class);
        when(where.and(any())).thenReturn(where);
        List<Integer> list = List.of(1, 2, 3);
        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.in("k", list)).thenReturn(mock(Clause.class));
            CassandraUtil.createWhereQuery("k", list, where);
        }
    }

    @Test
    void createWhereQueryHandlesSimpleValue() {
        Select.Where where = mock(Select.Where.class);
        when(where.and(any())).thenReturn(where);
        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.eq("k", "v")).thenReturn(mock(Clause.class));
            CassandraUtil.createWhereQuery("k", "v", where);
        }
    }

    @Test
    void createQueryHandlesMapListAndSimpleValue() {
        Select.Where where = mock(Select.Where.class);
        when(where.and(any())).thenReturn(where);

        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.lte("k", 5)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.lt("k", 4)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.gte("k", 2)).thenReturn(mock(Clause.class));
            qb.when(() -> QueryBuilder.gt("k", 1)).thenReturn(mock(Clause.class));
            CassandraUtil.createQuery("k", Map.of("lte", 5, "lt", 4, "gte", 2, "gt", 1), where);
        }

        List<Integer> list = List.of(1, 2, 3);
        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.in("k", list)).thenReturn(mock(Clause.class));
            CassandraUtil.createQuery("k", list, where);
        }

        try (MockedStatic<QueryBuilder> qb = mockStatic(QueryBuilder.class)) {
            qb.when(() -> QueryBuilder.eq("k", "v")).thenReturn(mock(Clause.class));
            CassandraUtil.createQuery("k", "v", where);
        }
    }

}
