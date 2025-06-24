package org.sunbird.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LocalizerTest {

    private static final String TEST_KEY = "test.key";
    private static final String TEST_VALUE = "Test Message";

    private static class DummyResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            if (TEST_KEY.equals(key)) return TEST_VALUE;
            throw new MissingResourceException("Missing", "Dummy", key);
        }
        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(Collections.singleton(TEST_KEY));
        }
    }

    private static class DummyControl extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException {
            return new DummyResourceBundle();
        }
    }

    @BeforeEach
    void setUp() {
        // Reset singleton for isolation
        try {
            java.lang.reflect.Field instance = Localizer.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getInstanceReturnsSingleton() {
        Localizer l1 = Localizer.getInstance();
        Localizer l2 = Localizer.getInstance();
        assertSame(l1, l2);
    }

    @Test
    void getLocaleReturnsCorrectLocale() {
        Localizer localizer = Localizer.getInstance();
        Locale locale = localizer.getLocale("en", "IN");
        assertEquals("en", locale.getLanguage());
        assertEquals("IN", locale.getCountry());
    }

    @Test
    void getMessageReturnsValueForDefaultLocale() {
        // Patch ResourceBundle to use dummy
        ResourceBundle.clearCache();
        Locale.setDefault(Locale.ENGLISH);
        ResourceBundle.Control control = new DummyControl();
        ResourceBundle bundle = ResourceBundle.getBundle("responseMessages", Locale.getDefault(), control);

        Localizer localizer = Localizer.getInstance();
        localizer.userResourceBundle = bundle;
        String msg = localizer.getMessage(TEST_KEY, null);
        assertEquals(TEST_VALUE, msg);
    }

    @Test
    void getMessageReturnsValueForGivenLocale() {
        ResourceBundle.clearCache();
        Locale locale = new Locale("fr", "FR");
        ResourceBundle.Control control = new DummyControl();
        ResourceBundle bundle = ResourceBundle.getBundle("responseMessages", locale, control);

        Localizer localizer = Localizer.getInstance();
        // Simulate initial bundle
        localizer.userResourceBundle = ResourceBundle.getBundle("responseMessages", Locale.ENGLISH, control);
        String msg = localizer.getMessage(TEST_KEY, locale);
        assertEquals(TEST_VALUE, msg);
    }
}