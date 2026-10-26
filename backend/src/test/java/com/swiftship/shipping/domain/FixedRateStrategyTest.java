package com.swiftship.shipping.domain;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedRateStrategyTest {
    private final ShippingPricingServiceFixture pricing = new ShippingPricingServiceFixture();

    @Test
    void calculatesStandardRate() {
        QuoteResponse quote = pricing.quote("STANDARD", "2.5", 100);
        assertEquals(new BigDecimal("13.00"), quote.total());
        assertEquals(new BigDecimal("3.00"), quote.weightCharge());
        assertEquals(new BigDecimal("5.00"), quote.distanceCharge());
    }

    @Test
    void calculatesExpressRate() {
        QuoteResponse quote = pricing.quote("EXPRESS", "2.5", 100);
        assertEquals(new BigDecimal("33.25"), quote.total());
        assertEquals(new BigDecimal("6.25"), quote.weightCharge());
        assertEquals(new BigDecimal("12.00"), quote.distanceCharge());
    }

    @Test
    void calculatesSameDayRateAtMaximumDistance() {
        QuoteResponse quote = pricing.quote("SAME_DAY", "2.5", 100);
        assertEquals(new BigDecimal("70.00"), quote.total());
        assertEquals(new BigDecimal("10.00"), quote.weightCharge());
        assertEquals(new BigDecimal("25.00"), quote.distanceCharge());
    }

    @Test
    void roundsEachChargeHalfUp() {
        QuoteResponse quote = pricing.quote("STANDARD", "0.125", 1);
        assertEquals(new BigDecimal("5.20"), quote.total());
    }

    @Test
    void rejectsSameDayDistancesAboveMaximumAsBusinessErrors() {
        ShippingException exception = assertThrows(ShippingException.class,
                () -> pricing.quote("SAME_DAY", "1", 101));
        assertEquals("SAME_DAY_DISTANCE_EXCEEDED", exception.code());
        assertEquals(422, exception.status());
    }

    @Test
    void returnsUnsupportedTierAsABusinessError() {
        ShippingException exception = assertThrows(ShippingException.class,
                () -> pricing.quote("OVERNIGHT", "1", 1));
        assertEquals("UNSUPPORTED_TIER", exception.code());
    }

    @Test
    void addingAnIndependentRateStrategyAutomaticallyExtendsSupportedTiers() {
        ShippingRateStrategy overnight = new ShippingRateStrategy() {
            @Override
            public String code() {
                return "OVERNIGHT";
            }

            @Override
            public com.swiftship.shipping.api.ApiModels.TierResponse details() {
                return new com.swiftship.shipping.api.ApiModels.TierResponse(
                        "OVERNIGHT", "Overnight", "Within 12 hours",
                        new BigDecimal("25.00"), new BigDecimal("1.00"), new BigDecimal("0.10"), null);
            }

            @Override
            public QuoteResponse quote(QuoteRequest request) {
                return new QuoteResponse("OVERNIGHT", "Within 12 hours",
                        new BigDecimal("25.00"), new BigDecimal("1.00"),
                        new BigDecimal("0.10"), new BigDecimal("26.10"));
            }
        };
        var extensiblePricing = new com.swiftship.shipping.application.ShippingPricingService(
                java.util.List.of(overnight));

        assertEquals("OVERNIGHT", extensiblePricing.listTiers().getFirst().code());
        assertEquals(new BigDecimal("26.10"), extensiblePricing.quote(
                new QuoteRequest(new BigDecimal("1"), 1, "OVERNIGHT")).total());
    }

    private static final class ShippingPricingServiceFixture {
        private final com.swiftship.shipping.application.ShippingPricingService service =
                new com.swiftship.shipping.application.ShippingPricingService(
                        java.util.List.of(new StandardRateStrategy(), new ExpressRateStrategy(), new SameDayRateStrategy()));

        QuoteResponse quote(String tier, String weight, int distance) {
            return service.quote(new QuoteRequest(new BigDecimal(weight), distance, tier));
        }
    }
}
