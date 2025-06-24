package org.sunbird.helper;

import org.junit.jupiter.api.Test;
import org.sunbird.cassandra.CassandraOperation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ServiceFactoryTest {

    @Test
    void testGetInstance_returnsNonNullInstance() {
        CassandraOperation instance = ServiceFactory.getInstance();
        assertNotNull(instance);
    }

    @Test
    void testGetInstance_returnsSameInstance() {
        CassandraOperation instance1 = ServiceFactory.getInstance();
        CassandraOperation instance2 = ServiceFactory.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testReadResolve_returnsSameInstanceAsGetInstance() throws Exception {
        // Access private constructor using reflection
        Constructor<ServiceFactory> constructor = ServiceFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ServiceFactory factoryInstance = constructor.newInstance();

        CassandraOperation viaReadResolve = factoryInstance.readResolve();
        CassandraOperation viaGetInstance = ServiceFactory.getInstance();
        assertSame(viaGetInstance, viaReadResolve);
    }

    @Test
    void testPrivateConstructor_coverageOnly() throws Exception {
        Constructor<ServiceFactory> constructor = ServiceFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ServiceFactory factoryInstance = constructor.newInstance();
        assertNotNull(factoryInstance);
    }

    @Test
    void testGetInstance_initializesOnlyOnce() throws Exception {
        // Clear static field to test re-initialization
        Field field = ServiceFactory.class.getDeclaredField("operation");
        field.setAccessible(true);
        field.set(null, null); // reset the singleton instance

        CassandraOperation instance1 = ServiceFactory.getInstance();
        CassandraOperation instance2 = ServiceFactory.getInstance();
        assertSame(instance1, instance2);
    }
}
