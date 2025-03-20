
package org.sunbird.cassandra.helper;

import com.datastax.driver.core.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.Constants;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.PropertiesCache;
import org.sunbird.message.ResponseCode;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        PropertiesCache.class,
        PoolingOptions.class,
        Cluster.class,
        Cluster.Builder.class,
        ConsistencyLevel.class,
        QueryLogger.class,
        Session.class,
        Metadata.class,
        Host.class,
        InetAddress.class
})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class ConnectionManagerImplTest {
 PropertiesCache cache = null;

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(PropertiesCache.class);
    cache = PowerMockito.mock(PropertiesCache.class);
    PowerMockito.when(PropertiesCache.getConfigValue(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn(null);
    when(PropertiesCache.getInstance()).thenReturn(cache);
    when(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)).thenReturn("1");
    when(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_LOCAl)).thenReturn("1");
    when(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
    when(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
    when(cache.getProperty(Constants.MAX_REQUEST_PER_CONNECTION)).thenReturn("1");
    when(cache.getProperty(Constants.HEARTBEAT_INTERVAL)).thenReturn("1");
    when(cache.getProperty(Constants.POOL_TIMEOUT)).thenReturn("1");
    when(cache.getProperty(Constants.QUERY_LOGGER_THRESHOLD)).thenReturn("1");


    PowerMockito.when(PropertiesCache.getConfigValue(Mockito.anyString())).thenReturn("some_value");
    PoolingOptions poolingOptions = PowerMockito.mock(PoolingOptions.class);
    PowerMockito.whenNew(PoolingOptions.class).withNoArguments().thenReturn(poolingOptions);
    PowerMockito.mockStatic(Cluster.class);
    Cluster cluster = PowerMockito.mock(Cluster.class);
    Cluster.Builder builder = PowerMockito.mock(Cluster.Builder.class);
    PowerMockito.when(Cluster.builder()).thenReturn(builder);
    PowerMockito.when(builder.addContactPoints(Mockito.anyString())).thenReturn(builder);
    PowerMockito.when(builder.withPort(Mockito.anyInt())).thenReturn(builder);
    PowerMockito.when(builder.withProtocolVersion(Mockito.anyObject())).thenReturn(builder);
    PowerMockito.when(builder.withRetryPolicy(Mockito.anyObject())).thenReturn(builder);
    PowerMockito.when(builder.withTimestampGenerator(Mockito.anyObject())).thenReturn(builder);
    PowerMockito.when(builder.withPoolingOptions(Mockito.anyObject())).thenReturn(builder);
    PowerMockito.when(builder.build()).thenReturn(cluster);
    QueryLogger queryLogger = PowerMockito.mock(QueryLogger.class);
    QueryLogger.Builder builder1 = PowerMockito.mock(QueryLogger.Builder.class);
    PowerMockito.when(builder1.withConstantThreshold(Mockito.anyLong())).thenReturn(builder1);
    PowerMockito.when(builder1.build()).thenReturn(queryLogger);
    PowerMockito.when(cluster.register(queryLogger)).thenReturn(cluster);
    Session cassandraSession = PowerMockito.mock(Session.class);
    PowerMockito.when(cluster.connect(Mockito.anyString())).thenReturn(cassandraSession);
    Metadata metadata = PowerMockito.mock(Metadata.class);
    PowerMockito.when(cluster.getMetadata()).thenReturn(metadata);
    PowerMockito.when(metadata.getClusterName()).thenReturn("cluster_name");
    Host host = PowerMockito.mock(Host.class);
    Set<Host> set = new HashSet<>();
    set.add(host);
    PowerMockito.when(metadata.getAllHosts()).thenReturn(set);
    PowerMockito.when(host.getDatacenter()).thenReturn("data_center");
    PowerMockito.when(host.getRack()).thenReturn("rack");
    InetAddress inetAddress = PowerMockito.mock(InetAddress.class);
    PowerMockito.when(host.getAddress()).thenReturn(inetAddress);
  }

  @Test
  public void testCreateConnectionSuccessWithoutUsernameAndPassword() throws Exception {
    boolean bool = new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE).createConnection("127.0.0.1", "9042", null, null, "cassandraKeySpace");
    assertEquals(true, bool);
  }

  @Test
  public void testCreateConnectionSuccessWithUserNameAndPassword() throws Exception {

    Boolean bool =
            new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE).createConnection("127.0.0.1", "9042", "cassandra", "password", "cassandraKeySpace");
    assertEquals(true, bool);
  }

  @Test
  public void testCreateConnectionFailure() {
    try {
     new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE).createConnection("127.0.0.1", "9042", "cassandra", "pass", "eySpace");
    } catch (Exception ex) {
    }
    assertTrue(500 == ResponseCode.SERVER_ERROR.getCode());
  }
}

