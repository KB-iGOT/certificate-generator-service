/**
 * 
 */
package org.sunbird.health.actor;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseActor;
import org.sunbird.JsonKeys;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.util.*;

/**
 * @author manzarul
 *
 */
public class HealthActor extends BaseActor{

	private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private static final RedisCacheUtil contentCache = new RedisCacheUtil();
	private Logger log = LoggerFactory.getLogger(getClass().getName());

	@Override
	public void onReceive(Request request) throws Throwable {
		Response response = new Response();
		response.getResult().put(JsonKeys.HEALTHY, true);
		List<Map<String, Object>> healthResults = new ArrayList<>();
		response.put(JsonKeys.CHECKS, healthResults);
		cassandraHealthStatus(response);
		redisHealthStatus(response);
		sender().tell(response, getSelf());
	}

	private void cassandraHealthStatus(Response response) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put(JsonKeys.NAME, JsonKeys.CASSANDRA_DB);
		Boolean res = true;
		Response cassandraQueryResponse = cassandraOperation.getRecordsByProperties(
				JsonKeys.SUNBIRD, JsonKeys.TABLE_SYSTEM_SETTINGS, null, null);
		if (cassandraQueryResponse.getResponseCode().getCode() != ResponseCode.OK.getCode()
				&& MapUtils.isNotEmpty(cassandraQueryResponse.getResult())) {
			res = false;
			response.put(JsonKeys.HEALTHY, res);
		}
		result.put(JsonKeys.HEALTHY, res);
		((List<Map<String, Object>>) response.get(JsonKeys.CHECKS)).add(result);
	}

	private void redisHealthStatus(Response response) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put(JsonKeys.NAME, JsonKeys.REDIS_CACHE);
		Boolean res = true;
		Set<String> redisResponse = contentCache.getAllKeys();
		if (redisResponse == null || redisResponse.isEmpty()) {
			res = false;
			response.put(JsonKeys.HEALTHY, res);
		}
		result.put(JsonKeys.HEALTHY, res);
		((List<Map<String, Object>>) response.get(JsonKeys.CHECKS)).add(result);
	}

}
