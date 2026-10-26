package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippingValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsMinimumAndMaximumWeightAndDistanceValues() {
        assertTrue(validator.validate(new QuoteRequest(new BigDecimal("0.1"), 1, "STANDARD")).isEmpty());
        assertTrue(validator.validate(new QuoteRequest(new BigDecimal("50.0"), 2500, "EXPRESS")).isEmpty());
    }

    @Test
    void rejectsWeightsOutsideInclusiveBoundaries() {
        assertFalse(validator.validate(new QuoteRequest(new BigDecimal("0.09"), 1, "STANDARD")).isEmpty());
        assertFalse(validator.validate(new QuoteRequest(new BigDecimal("50.1"), 1, "STANDARD")).isEmpty());
    }

    @Test
    void rejectsDistancesOutsideInclusiveBoundaries() {
        assertFalse(validator.validate(new QuoteRequest(new BigDecimal("1"), 0, "STANDARD")).isEmpty());
        assertFalse(validator.validate(new QuoteRequest(new BigDecimal("1"), 2501, "STANDARD")).isEmpty());
    }

    @Test
    void rejectsMissingFields() {
        assertFalse(validator.validate(new QuoteRequest(null, null, null)).isEmpty());
    }
}
