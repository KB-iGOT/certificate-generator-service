package org.sunbird.cassandraimpl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class CassandraDACImpl extends CassandraOperationImpl {

  Logger logger = LoggerFactory.getLogger(CassandraDACImpl.class);
  public Response getRecords(
      String keySpace, String table, Map<String, Object> filters, List<String> fields) throws BaseException {
    Response response = new Response();
    Session session = connectionManager.getSession(keySpace);
    try {
      Select select;
      if (CollectionUtils.isNotEmpty(fields)) {
        select = QueryBuilder.select((String[]) fields.toArray()).from(keySpace, table);
      } else {
        select = QueryBuilder.select().all().from(keySpace, table);
      }

      if (MapUtils.isNotEmpty(filters)) {
        Select.Where where = select.where();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
          Object value = filter.getValue();
          if (value instanceof List) {
            where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
          } else {
            where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
          }
        }
      }

      ResultSet results = null;
      results = session.execute(select);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(Constants.EXCEPTION_MSG_FETCH + table + " : " + e.getMessage(), e);
      throw new BaseException(
              IResponseMessage.SERVER_ERROR,
              IResponseMessage.SERVER_ERROR,
              ResponseCode.SERVER_ERROR.getCode());
    }
    return response;
  }

  public void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback) throws BaseException {
    Session session = connectionManager.getSession(keySpace);
    try {
      Select select;
      if (CollectionUtils.isNotEmpty(fields)) {
        select = QueryBuilder.select((String[]) fields.toArray()).from(keySpace, table);
      } else {
        select = QueryBuilder.select().all().from(keySpace, table);
      }

      if (MapUtils.isNotEmpty(filters)) {
        Select.Where where = select.where();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
          Object value = filter.getValue();
          if (value instanceof List) {
            where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
          } else {
            where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
          }
        }
      }
      ResultSetFuture future = session.executeAsync(select);
      Futures.addCallback(future, callback, Executors.newFixedThreadPool(1));
    } catch (Exception e) {
      logger.error(Constants.EXCEPTION_MSG_FETCH + table + " : " + e.getMessage(), e);
      throw new BaseException(
              IResponseMessage.SERVER_ERROR,
              IResponseMessage.SERVER_ERROR,
              ResponseCode.SERVER_ERROR.getCode());
    }
  }

  public Response updateAddMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value) throws BaseException {
    return updateMapRecord(keySpace, table, primaryKey, column, key, value, true);
  }

  public Response updateRemoveMapRecord(
      String keySpace, String table, Map<String, Object> primaryKey, String column, String key) throws BaseException {
    return updateMapRecord(keySpace, table, primaryKey, column, key, null, false);
  }

  public Response updateMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value,
      boolean add) throws BaseException {
    Update update = QueryBuilder.update(keySpace, table);
    if (add) {
      update.with(QueryBuilder.put(column, key, value));
    } else {
      update.with(QueryBuilder.remove(column, key));
    }
    if (MapUtils.isEmpty(primaryKey)) {
      logger.error(
          Constants.EXCEPTION_MSG_FETCH + table + " : primary key is a must for update call");
      throw new BaseException(
              IResponseMessage.SERVER_ERROR,
              IResponseMessage.SERVER_ERROR,
              ResponseCode.SERVER_ERROR.getCode());
    }
    Update.Where where = update.where();
    for (Map.Entry<String, Object> filter : primaryKey.entrySet()) {
      Object filterValue = filter.getValue();
      if (filterValue instanceof List) {
        where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
      } else {
        where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
      }
    }
    try {
      Response response = new Response();
      logger.debug("Remove Map-Key Query: " + update.toString());
      connectionManager.getSession(keySpace).execute(update);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
      return response;
    } catch (Exception e) {
      logger.error(Constants.EXCEPTION_MSG_FETCH + table + " : " + e.getMessage(), e);
      throw new BaseException(
              IResponseMessage.SERVER_ERROR,
              IResponseMessage.SERVER_ERROR,
              ResponseCode.SERVER_ERROR.getCode());
    }
  }
}
