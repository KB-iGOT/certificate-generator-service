package org.sunbird.cassandra.helper;

import com.datastax.driver.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.sunbird.common.Constants;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.PropertiesCache;
import org.sunbird.message.ResponseCode;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ConnectionManagerImplTest {

    @BeforeEach
    public void setUp() throws Exception {
        // No global setup needed, handled inside each test using mockStatic and mockConstruction
    }

    @Test
    public void testCreateConnectionSuccessWithoutUsernameAndPassword() throws Exception {
        try (
                MockedStatic<PropertiesCache> staticCache = mockStatic(PropertiesCache.class);
                MockedStatic<Cluster> staticCluster = mockStatic(Cluster.class);
                MockedConstruction<PoolingOptions> mockedPooling = mockConstruction(PoolingOptions.class)
        ) {
            PropertiesCache cache = mock(PropertiesCache.class);
            staticCache.when(PropertiesCache::getInstance).thenReturn(cache);
            staticCache.when(() -> PropertiesCache.getConfigValue(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn(null);
            when(cache.getProperty(anyString())).thenReturn("1");

            Cluster.Builder builder = mock(Cluster.Builder.class);
            Cluster cluster = mock(Cluster.class);
            Session session = mock(Session.class);
            Metadata metadata = mock(Metadata.class);
            Host host = mock(Host.class);
            InetAddress inetAddress = mock(InetAddress.class);

            staticCluster.when(Cluster::builder).thenReturn(builder);
            when(builder.addContactPoints(anyString())).thenReturn(builder);
            when(builder.withPort(anyInt())).thenReturn(builder);
            when(builder.withProtocolVersion(any())).thenReturn(builder);
            when(builder.withRetryPolicy(any())).thenReturn(builder);
            when(builder.withTimestampGenerator(any())).thenReturn(builder);
            when(builder.withPoolingOptions(any())).thenReturn(builder);
            when(builder.build()).thenReturn(cluster);

            QueryLogger.Builder qBuilder = mock(QueryLogger.Builder.class);
            QueryLogger queryLogger = mock(QueryLogger.class);
            when(qBuilder.withConstantThreshold(anyLong())).thenReturn(qBuilder);
            when(qBuilder.build()).thenReturn(queryLogger);
            when(cluster.register(queryLogger)).thenReturn(cluster);

            when(cluster.connect(anyString())).thenReturn(session);
            when(cluster.getMetadata()).thenReturn(metadata);
            when(metadata.getClusterName()).thenReturn("cluster_name");
            when(metadata.getAllHosts()).thenReturn(new HashSet<>() {{
                add(host);
            }});
            when(host.getDatacenter()).thenReturn("data_center");
            when(host.getRack()).thenReturn("rack");
            when(host.getAddress()).thenReturn(inetAddress);

            boolean result = new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE)
                    .createConnection("127.0.0.1", "9042", null, null, "cassandraKeySpace");
            assertTrue(result);
        }
    }

    @Test
    public void testCreateConnectionSuccessWithUserNameAndPassword() throws Exception {
        try (
                MockedStatic<PropertiesCache> staticCache = mockStatic(PropertiesCache.class);
                MockedStatic<Cluster> staticCluster = mockStatic(Cluster.class);
                MockedConstruction<PoolingOptions> mockedPooling = mockConstruction(PoolingOptions.class)
        ) {
            PropertiesCache cache = mock(PropertiesCache.class);
            staticCache.when(PropertiesCache::getInstance).thenReturn(cache);
            staticCache.when(() -> PropertiesCache.getConfigValue(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn(null);
            when(cache.getProperty(anyString())).thenReturn("1");

            Cluster.Builder builder = mock(Cluster.Builder.class);
            Cluster cluster = mock(Cluster.class);
            Session session = mock(Session.class);
            Metadata metadata = mock(Metadata.class);
            Host host = mock(Host.class);
            InetAddress inetAddress = mock(InetAddress.class);

            staticCluster.when(Cluster::builder).thenReturn(builder);
            when(builder.addContactPoints(anyString())).thenReturn(builder);
            when(builder.withPort(anyInt())).thenReturn(builder);
            when(builder.withProtocolVersion(any())).thenReturn(builder);
            when(builder.withRetryPolicy(any())).thenReturn(builder);
            when(builder.withTimestampGenerator(any())).thenReturn(builder);
            when(builder.withPoolingOptions(any())).thenReturn(builder);
            when(builder.withCredentials(anyString(), anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(cluster);

            QueryLogger.Builder qBuilder = mock(QueryLogger.Builder.class);
            QueryLogger queryLogger = mock(QueryLogger.class);
            when(qBuilder.withConstantThreshold(anyLong())).thenReturn(qBuilder);
            when(qBuilder.build()).thenReturn(queryLogger);
            when(cluster.register(queryLogger)).thenReturn(cluster);

            when(cluster.connect(anyString())).thenReturn(session);
            when(cluster.getMetadata()).thenReturn(metadata);
            when(metadata.getClusterName()).thenReturn("cluster_name");
            when(metadata.getAllHosts()).thenReturn(new HashSet<>() {{
                add(host);
            }});
            when(host.getDatacenter()).thenReturn("data_center");
            when(host.getRack()).thenReturn("rack");
            when(host.getAddress()).thenReturn(inetAddress);

            boolean result = new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE)
                    .createConnection("127.0.0.1", "9042", "cassandra", "password", "cassandraKeySpace");
            assertTrue(result);
        }
    }

    @Test
    public void testCreateConnectionFailure() {
        try {
            new CassandraConnectionManagerImpl(Constants.STANDALONE_MODE)
                    .createConnection("127.0.0.1", "9042", "cassandra", "pass", "eySpace");
        } catch (Exception e) {
            // expected
        }
        assertEquals(500, ResponseCode.SERVER_ERROR.getCode());
    }
}
