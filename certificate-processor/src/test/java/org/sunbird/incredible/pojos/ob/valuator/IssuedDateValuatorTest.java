package org.sunbird.incredible.pojos.ob.valuator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.ob.exeptions.InvalidDateFormatException;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

class IssuedDateValuatorTest {

    private IssuedDateValuator issuedDateValuator;
    private SimpleDateFormat simpleDateFormat;
    private Calendar cal;

    @BeforeEach
    void setUp() {
        issuedDateValuator = new IssuedDateValuator();
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        cal = Calendar.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Cleanup logic if necessary
    }

    @Test
    void evaluatesDateInNullException() {
        InvalidDateFormatException exception = assertThrows(
                InvalidDateFormatException.class,
                () -> issuedDateValuator.convertToDate(null)
        );
        assertEquals("issued date cannot be null", exception.getMessage(), "Custom message check optional");
    }

    @Test
    void evaluatesIssuedDateInExceptionForDifferentFormats() {
        InvalidDateFormatException exception = assertThrows(
                InvalidDateFormatException.class,
                () -> issuedDateValuator.convertToDate("2019-02")
        );
        assertEquals("issued date is not in valid format", exception.getMessage(), "Custom message check optional");
    }


    @Test
    void testEvaluatesWithFullDateTimeFormat() throws InvalidDateFormatException {
        String input = "2023-06-15T10:30:00Z";
        String result = issuedDateValuator.evaluates(input);
        assertEquals("2023-06-15T10:30:00Z", result);
    }

    @Test
    void testEvaluatesWithDateOnlyFormat() throws InvalidDateFormatException {
        String input = "2023-06-15";
        String result = issuedDateValuator.evaluates(input);
        assertEquals("2023-06-15T00:00:00Z", result);
    }
}
