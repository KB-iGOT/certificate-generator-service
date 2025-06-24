package org.sunbird.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertiesCacheTest {

    private Properties testProps;

    @BeforeEach
    void setup() {
        testProps = new Properties();
        testProps.setProperty("test.key", "test.value");
        testProps.setProperty("another.key", "another.value");

        // Inject test properties into the singleton instance
        PropertiesCache cache = PropertiesCache.getInstance();
        injectProps(cache, testProps);
    }

    @Test
    void testSingletonInstance() {
        PropertiesCache instance1 = PropertiesCache.getInstance();
        PropertiesCache instance2 = PropertiesCache.getInstance();
        assertSame(instance1, instance2, "Should return same instance");
    }

    @Test
    void testGetProperty_existingKey() {
        PropertiesCache cache = PropertiesCache.getInstance();
        assertEquals("test.value", cache.getProperty("test.key"));
    }

    @Test
    void testGetProperty_keyNotFound_returnsKeyItself() {
        PropertiesCache cache = PropertiesCache.getInstance();
        assertEquals("missing.key", cache.getProperty("missing.key"));
    }

    @Test
    void testReadProperty_fallbackToProperties_only() {
        // This test relies on environment variable being null
        PropertiesCache cache = PropertiesCache.getInstance();
        assertEquals("another.value", cache.readProperty("another.key"));
    }

    @Test
    void testGetConfigValue_fallbackToProperties_only() {
        // This test assumes env is null
        assertEquals("another.value", PropertiesCache.getConfigValue("another.key"));
    }

    // --- Utility to inject props ---
    private void injectProps(PropertiesCache cache, Properties newProps) {
        try {
            Field field = PropertiesCache.class.getDeclaredField("configProp");
            field.setAccessible(true);
            Properties configProps = (Properties) field.get(cache);
            configProps.clear();
            configProps.putAll(newProps);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
