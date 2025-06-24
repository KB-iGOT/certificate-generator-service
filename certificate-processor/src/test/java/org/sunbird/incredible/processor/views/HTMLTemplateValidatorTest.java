package org.sunbird.incredible.processor.views;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HTMLTemplateValidatorTest {

    @Test
    public void testValidateWithValidAndInvalidVariables() {
        String html = "${name} ${age} ${invalidVar}";
        try (MockedStatic<HTMLVars> mocked = mockStatic(HTMLVars.class)) {
            mocked.when(HTMLVars::get).thenReturn(Arrays.asList("${name}", "${age}"));

            HTMLTemplateValidator validator = new HTMLTemplateValidator(html);
            Set<String> result = validator.validate();

            assertEquals(1, result.size());
            assertTrue(result.contains("${invalidVar}"));
        }
    }

    @Test
    public void testValidateWithAllValidVariables() {
        String html = "${name} ${age}";
        try (MockedStatic<HTMLVars> mocked = mockStatic(HTMLVars.class)) {
            mocked.when(HTMLVars::get).thenReturn(Arrays.asList("${name}", "${age}"));

            HTMLTemplateValidator validator = new HTMLTemplateValidator(html);
            Set<String> result = validator.validate();

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testStoreAllHTMLTemplateVariablesWithValidHTML() {
        String html = "${name} ${email}";
        Set<String> result = HTMLTemplateValidator.storeAllHTMLTemplateVariables(html);
        assertTrue(result.contains("${name}"));
        assertTrue(result.contains("${email}"));
    }

}
