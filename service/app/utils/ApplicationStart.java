package utils;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.auth.KeyManager;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import play.api.Environment;
import play.api.inject.ApplicationLifecycle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * This class will be called after on application startup. only one instance of this class will be
 * created. StartModule class has responsibility to eager load this class.
 *
 * @author manzarul
 */
@Singleton
public class ApplicationStart {

	private static Logger logger = LoggerFactory.getLogger(ApplicationStart.class);
	private static Localizer localizer = Localizer.getInstance();
	  /**
	   * All one time initialization which required during server startup will fall here.
	   * @param lifecycle ApplicationLifecycle
	   * @param environment Environment
	   */
	  @Inject
	  public ApplicationStart(ApplicationLifecycle lifecycle, Environment environment) throws BaseException {
		  createCassandraConnection(JsonKeys.SUNBIRD);
		  createCassandraConnection(JsonKeys.COURSE_KEY_SPACE_NAME);
		  System.out.println("keymanger.init():starts");
		  KeyManager.init();
		  // Shut-down hook
	    lifecycle.addStopHook(
	        () -> {
	          return CompletableFuture.completedFuture(null);
	        });
	  }

	/**
	 * This method will read the configuration from System variable.
	 *
	 * @return boolean
	 */
	public static boolean createCassandraConnection(String keyspace) throws BaseException {
		boolean response = false;
		String ips = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_IP);
		String envPort = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_PORT);
		CassandraConnectionManager cassandraConnectionManager =
				CassandraConnectionMngrFactory.getObject(JsonKeys.STANDALONE_MODE);

		if (StringUtils.isBlank(ips) || StringUtils.isBlank(envPort)) {
			logger.info("Configuration value is not coming form System variable.");
			return false;
		}
		String[] portList = envPort.split(",");
		String userName = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_USER_NAME);
		String password = System.getenv(JsonKeys.SUNBIRD_CASSANDRA_PASSWORD);
		try {
			boolean result =
					cassandraConnectionManager.createConnection(ips, portList[0], userName, password, keyspace);
			if (result) {
				response = true;
				logger.info(
						"CONNECTION CREATED SUCCESSFULLY FOR IP's: " + ips + " : KEYSPACE :" + keyspace);
			} else {
				logger.info(
						"CONNECTION CREATION FAILED FOR IP: " + ips + " : KEYSPACE :" + keyspace);
			}
		} catch (BaseException ex) {
			logger.error("Application:createCassandraConnection: Exception occurred with message = " + ex.getMessage());
		}
		if (!response) {
			throw new BaseException(
					IResponseMessage.INVALID_CONFIGURATION,
					getLocalizedMessage(IResponseMessage.INVALID_CONFIGURATION,null),
					ResponseCode.SERVER_ERROR.hashCode());
		}
		return response;
	}

	private static String getLocalizedMessage(String key, Locale locale){
		return localizer.getMessage(key, locale);
	}

}
