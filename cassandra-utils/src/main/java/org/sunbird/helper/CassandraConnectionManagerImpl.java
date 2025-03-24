package org.sunbird.helper;

import com.datastax.driver.core.AtomicMonotonicTimestampGenerator;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryLogger;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.policies.DefaultRetryPolicy;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.common.Constants;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;

/**
 * @author Amit Kumar
 * @author Arvind
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {

  private static Logger logger = LoggerFactory.getLogger(CassandraConnectionManagerImpl.class);
  private String mode;
  private static Map<String, Session> cassandraSessionMap = new HashMap<>();
  private static Map<String, Cluster> cassandraclusterMap = new HashMap<>();
  private static Localizer localizer = Localizer.getInstance();

  static {
    registerShutDownHook();
  }

  public CassandraConnectionManagerImpl(String mode) {
    this.mode = mode;
  }

  /**
   * Method to create the cassandra connection on the basis of mode i.e. standalone or embedde read
   * from properties file .
   *
   * @param ip
   * @param port
   * @param userName
   * @param password
   * @param keyspace
   * @return boolean
   */
  @Override
  public boolean createConnection(
      String ip, String port, String userName, String password, String keyspace) throws BaseException {
    return createStandaloneConnection(ip, port, userName, password, keyspace);
  }

  /**
   * Method to create the standalone cassandra connection .
   *
   * @param ips
   * @param port
   * @param userName
   * @param password
   * @param keyspace
   * @return
   */
  private boolean createStandaloneConnection(
      String ips, String port, String userName, String password, String keyspace) throws BaseException {

    Session cassandraSession = null;
    boolean connection = false;
    Cluster cluster = null;
    try {
      if (null == cassandraSessionMap.get(keyspace)) {
        PropertiesCache cache = PropertiesCache.getInstance();
        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions.setCoreConnectionsPerHost(
            HostDistance.LOCAL,
            Integer.parseInt(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)));
        poolingOptions.setMaxConnectionsPerHost(
            HostDistance.LOCAL,
            Integer.parseInt(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_LOCAl)));
        poolingOptions.setCoreConnectionsPerHost(
            HostDistance.REMOTE,
            Integer.parseInt(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)));
        poolingOptions.setMaxConnectionsPerHost(
            HostDistance.REMOTE,
            Integer.parseInt(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_REMOTE)));
        poolingOptions.setMaxRequestsPerConnection(
            HostDistance.LOCAL,
            Integer.parseInt(cache.getProperty(Constants.MAX_REQUEST_PER_CONNECTION)));
        poolingOptions.setHeartbeatIntervalSeconds(
            Integer.parseInt(cache.getProperty(Constants.HEARTBEAT_INTERVAL)));
        poolingOptions.setPoolTimeoutMillis(
            Integer.parseInt(cache.getProperty(Constants.POOL_TIMEOUT)));
        if (!StringUtils.isBlank(userName) && !StringUtils.isBlank(password)) {
          cluster = createCluster(ips, port, userName, password, poolingOptions);
        } else {
          cluster = createCluster(ips, port, poolingOptions);
        }
        QueryLogger queryLogger =
            QueryLogger.builder()
                .withConstantThreshold(
                    Integer.parseInt(cache.getProperty(Constants.QUERY_LOGGER_THRESHOLD)))
                .build();

        cluster.register(queryLogger);
        cassandraSession = cluster.connect(keyspace);

        if (null != cassandraSession) {
          connection = true;
          cassandraSessionMap.put(keyspace, cassandraSession);
          cassandraclusterMap.put(keyspace, cluster);
        }
        final Metadata metadata = cluster.getMetadata();
        String msg = String.format("Connected to cluster: %s", metadata.getClusterName());
        logger.debug(msg);

        for (final Host host : metadata.getAllHosts()) {
          msg =
              String.format(
                  "Datacenter: %s; Host: %s; Rack: %s",
                  host.getDatacenter(), host.getAddress(), host.getRack());
          logger.info(msg);
        }
      }
    } catch (Exception e) {
      logger.error("Error occured while creating connection :", e);
      throw new BaseException(
          IResponseMessage.INTERNAL_ERROR,
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getCode());
    }

    if (null != cassandraSessionMap.get(keyspace)) {
      connection = true;
    }
    return connection;
  }

  private static ConsistencyLevel getConsistencyLevel() {
    String consistency = PropertiesCache.getConfigValue(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL);

    logger.info(
        "CassandraConnectionManagerImpl:getConsistencyLevel: level = " + consistency);

    if (StringUtils.isBlank(consistency)) return null;

    try {
      return ConsistencyLevel.valueOf(consistency.toUpperCase());
    } catch (IllegalArgumentException exception) {
      logger.error(
          "CassandraConnectionManagerImpl:getConsistencyLevel: Exception occurred with error message = "
              + exception.getMessage());
    }
    return null;
  }

  /**
   * Create cassandra cluster with user credentials.
   *
   * @param ips IP address of cluster node
   * @param port Port of cluster node
   * @param userName DB username
   * @param password DB password
   * @param poolingOptions Pooling options
   * @return Cassandra cluster
   */
  private static Cluster createCluster(
      String ips, String port, String userName, String password, PoolingOptions poolingOptions) {
    String[] hosts =  null;
    if (StringUtils.isNotBlank(ips)) {
      hosts =  ips.split(",");
    } else {
      hosts = new String[] { "localhost" };
    }
    Cluster.Builder builder =
        Cluster.builder()
            .addContactPoints(hosts)
            .withPort(Integer.parseInt(port))
            .withProtocolVersion(ProtocolVersion.V3)
            .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
            .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
            .withPoolingOptions(poolingOptions);

    if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
      builder.withCredentials(userName, password);
    }

    ConsistencyLevel consistencyLevel = getConsistencyLevel();
    logger.info(
        "CassandraConnectionManagerImpl:createCluster: Consistency level = " + consistencyLevel);

    if (consistencyLevel != null) {
      builder.withQueryOptions(new QueryOptions().setConsistencyLevel(consistencyLevel));
    }

    return builder.build();
  }

  /**
   * Create cassandra cluster.
   *
   * @param ip IP address of cluster node
   * @param port Port of cluster node
   * @param poolingOptions Pooling options
   * @return Cassandra cluster
   */
  private static Cluster createCluster(String ip, String port, PoolingOptions poolingOptions) {
    return createCluster(ip, port, null, null, poolingOptions);
  }

  @Override
  public Session getSession(String keyspaceName) throws BaseException {
    if (null == cassandraSessionMap.get(keyspaceName)) {
      createCassandraConnection(keyspaceName);
    }
    return cassandraSessionMap.get(keyspaceName);
  }

  @Override
  public Cluster getCluster(String keyspaceName) throws BaseException {
    if (null == cassandraclusterMap.get(keyspaceName)) {
      throw new BaseException(
          IResponseMessage.INTERNAL_ERROR,
          Constants.CLUSTER_IS_NULL + keyspaceName,
          ResponseCode.SERVER_ERROR.getCode());
    }
    return cassandraclusterMap.get(keyspaceName);
  }

  @Override
  public List<String> getTableList(String keyspacename) {
    Collection<TableMetadata> tables =
        cassandraclusterMap.get(keyspacename).getMetadata().getKeyspace(keyspacename).getTables();

    // to convert to list of the names
    return tables.stream().map(tm -> tm.getName()).collect(Collectors.toList());
  }

  /** Register the hook for resource clean up. this will be called when jvm shut down. */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    logger.info("Cassandra ShutDownHook registered.");
  }

  /**
   * This class will be called by registerShutDownHook to register the call inside jvm , when jvm
   * terminate it will call the run method to clean up the resource.
   */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      logger.info("started resource cleanup Cassandra.");
      for (Map.Entry<String, Session> entry : cassandraSessionMap.entrySet()) {
        cassandraSessionMap.get(entry.getKey()).close();
        cassandraclusterMap.get(entry.getKey()).close();
      }
      logger.info("completed resource cleanup Cassandra.");
    }
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
      logger.error("createCassandraConnection: Exception occurred with message = " + ex.getMessage());
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
