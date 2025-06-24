package org.sunbird.health.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cert.helper.IssueCertificateEventHelper;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.lang.reflect.Field;
import java.util.*;

import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthActorTest {

    private static ActorSystem system;
    private static CassandraOperation cassandraOperation;
    private static RedisCacheUtil contentCache;
    private static MockedStatic<ServiceFactory> serviceFactoryMockedStatic;

    @BeforeAll
    static void beforeAll() {
        system = ActorSystem.create("test-system");

        cassandraOperation = mock(CassandraOperation.class);
        contentCache = mock(RedisCacheUtil.class);

        // Mock static call to ServiceFactory.getInstance()
        serviceFactoryMockedStatic = mockStatic(ServiceFactory.class);
        serviceFactoryMockedStatic.when(ServiceFactory::getInstance).thenReturn(cassandraOperation);

        // Inject mocks using Unsafe for static final fields
        setFinalStatic(HealthActor.class, "cassandraOperation", cassandraOperation);
        setFinalStatic(HealthActor.class, "contentCache", contentCache);
    }

    @AfterAll
    static void afterAll() {
        TestKit.shutdownActorSystem(system);
        serviceFactoryMockedStatic.close();
        system = null;
    }

    @BeforeEach
    void resetMocks() {
        reset(cassandraOperation, contentCache);
    }

    @Test
    void testHealthSuccess() throws BaseException {
        setFinalStatic(HealthActor.class, "cassandraOperation", cassandraOperation);
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(createMockResponse(true));
        when(contentCache.getAllKeys()).thenReturn(Set.of("key1"));

        TestKit probe = new TestKit(system);
        ActorRef actorRef = system.actorOf(Props.create(HealthActor.class));
        actorRef.tell(new Request(), probe.getRef());

        Response response = probe.expectMsgClass(Response.class);

        assertEquals(2, ((List<?>) response.getResult().get(JsonKeys.CHECKS)).size());
    }

    @Test
    void testCassandraHealthFailure() throws BaseException {
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));
        when(contentCache.getAllKeys()).thenReturn(Set.of("key1"));

        TestKit probe = new TestKit(system);
        ActorRef actorRef = system.actorOf(Props.create(HealthActor.class));
        actorRef.tell(new Request(), probe.getRef());

        Response response = probe.expectMsgClass(Response.class);

        assertFalse((Boolean) response.getResult().get(JsonKeys.HEALTHY));
    }

    @Test
    void testRedisHealthFailure() throws BaseException {
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(createMockResponse(true));
        when(contentCache.getAllKeys()).thenReturn(Collections.emptySet());

        TestKit probe = new TestKit(system);
        ActorRef actorRef = system.actorOf(Props.create(HealthActor.class));
        actorRef.tell(new Request(), probe.getRef());

        Response response = probe.expectMsgClass(Response.class);

        assertFalse((Boolean) response.getResult().get(JsonKeys.HEALTHY));
    }

    @Test
    void testRedisHealthException() throws BaseException {
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(createMockResponse(true));
        when(contentCache.getAllKeys()).thenThrow(new RuntimeException("Redis fail"));

        TestKit probe = new TestKit(system);
        ActorRef actorRef = system.actorOf(Props.create(HealthActor.class));
        actorRef.tell(new Request(), probe.getRef());

        Response response = probe.expectMsgClass(Response.class);

        assertFalse((Boolean) response.getResult().get(JsonKeys.HEALTHY));
    }

    private Response createMockResponse(boolean healthy) {
        Response response = new Response();
        if (healthy) {
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("key", "value");
            response.getResult().put(JsonKeys.RESPONSE, mockData);
        }
        return response;
    }

    // === 🔧 Reflection Unsafe Setter ===
    private static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            Object base = unsafe.staticFieldBase(field);
            long offset = unsafe.staticFieldOffset(field);
            unsafe.putObject(base, offset, newValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set final static field: " + fieldName, e);
        }
    }
}
