package org.sunbird.incredible.pojos.ob.valuator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.ob.exeptions.InvalidDateFormatException;

import static org.junit.jupiter.api.Assertions.*;

public class ExpiryDateValuatorTest {

    private String issuedDate;

    @BeforeEach
    public void setUp() {
        issuedDate = "2019-08-31T12:52:25Z";
    }

    @AfterEach
    public void tearDown() {
        // Cleanup if needed
    }

    @Test
    public void expiryDateIsCorrectForMonths() throws InvalidDateFormatException {
        String expiryDate = "2m";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2019-10-31T12:52:25Z", expiry, "expiry date is valid for months");
    }

    @Test
    public void expiryDateIsCorrectForYears() throws InvalidDateFormatException {
        String expiryDate = "2Y";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2021-08-31T12:52:25Z", expiry, "expiry date is valid for years");
    }

    @Test
    public void expiryDateIsCorrectForDays() throws InvalidDateFormatException {
        String expiryDate = "2d";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2019-09-02T12:52:25Z", expiry, "expiry date is valid for days");
    }

    @Test
    public void expiryDateIsCorrectForDaysAndYears() throws InvalidDateFormatException {
        String expiryDate = "2D 2y";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2021-09-02T12:52:25Z", expiry, "expiry date is valid for both days and years");
    }

    @Test
    public void expiryDateIsCorrectForMonthsAndYears() throws InvalidDateFormatException {
        String expiryDate = "2M 1y";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2020-10-31T12:52:25Z", expiry, "expiry date is valid for both months and years");
    }

    @Test
    public void expiryDateIsCorrectForDaysAndMonths() throws InvalidDateFormatException {
        String expiryDate = "2d 2m";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2019-11-02T12:52:25Z", expiry, "expiry date is valid for both days and months");
    }

    @Test
    public void expiryDateIsCorrectInFormat() throws InvalidDateFormatException {
        String expiryDate = "2019-09-02T12:52:25Z";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);
        String expiry = expiryDateValuator.getExpiryDate(expiryDate);
        assertEquals("2019-09-02T12:52:25Z", expiry, "expiry date is correctly formatted");
    }

    @Test
    public void expiryDateIsInvalidFormatThrowsException() {
        String expiryDate = "2019-34-12";
        ExpiryDateValuator expiryDateValuator = new ExpiryDateValuator(issuedDate);

        InvalidDateFormatException exception = assertThrows(
                InvalidDateFormatException.class,
                () -> expiryDateValuator.getExpiryDate(expiryDate),
                "Expected InvalidDateFormatException for invalid date"
        );

        assertNotNull(exception.getMessage());
    }
}
