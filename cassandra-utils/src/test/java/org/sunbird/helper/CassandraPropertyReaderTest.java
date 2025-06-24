package org.sunbird.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CassandraPropertyReaderTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = CassandraPropertyReader.class.getDeclaredField("cassandraPropertyReader");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSingletonInstance() {
        CassandraPropertyReader instance1 = CassandraPropertyReader.getInstance();
        CassandraPropertyReader instance2 = CassandraPropertyReader.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "Should return the same singleton instance");
    }

    @Test
    void testReadProperty_keyExists() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        String value = reader.readProperty("test.key");
        assertEquals("test.value", value);
    }

    @Test
    void testReadProperty_keyDoesNotExist_returnsKeyItself() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        String value = reader.readProperty("non.existing.key");
        assertEquals("non.existing.key", value);
    }

    @Test
    void testConstructor_propertyFileMissing_gracefulFallback() throws Exception {
        // Reset the singleton to force re-initialization
        Field instanceField = CassandraPropertyReader.class.getDeclaredField("cassandraPropertyReader");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        ClassLoader customClassLoader = new ClassLoader() {
            @Override
            public InputStream getResourceAsStream(String name) {
                return null; // Simulate file not found
            }
        };

        Class<?> clazz = Class.forName("org.sunbird.helper.CassandraPropertyReader", true, customClassLoader);
        Object instance = clazz.getDeclaredMethod("getInstance").invoke(null);

        // Should not throw exception and return non-null instance
        assertNotNull(instance);
    }
}
