package com.swiftship.shipping.domain;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.api.ApiModels.TierResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

abstract class FixedRateStrategy implements ShippingRateStrategy {
    private final String code;
    private final String name;
    private final String deliveryEstimate;
    private final Integer maximumDistanceKm;
    private final BigDecimal baseFee;
    private final BigDecimal ratePerKg;
    private final BigDecimal ratePerKm;

    FixedRateStrategy(
            String code,
            String name,
            String deliveryEstimate,
            Integer maximumDistanceKm,
            String baseFee,
            String ratePerKg,
            String ratePerKm) {
        this.code = code;
        this.name = name;
        this.deliveryEstimate = deliveryEstimate;
        this.maximumDistanceKm = maximumDistanceKm;
        this.baseFee = new BigDecimal(baseFee);
        this.ratePerKg = new BigDecimal(ratePerKg);
        this.ratePerKm = new BigDecimal(ratePerKm);
    }

    @Override
    public final String code() {
        return code;
    }

    @Override
    public final TierResponse details() {
        return new TierResponse(code, name, deliveryEstimate, baseFee, ratePerKg, ratePerKm, maximumDistanceKm);
    }

    @Override
    public final QuoteResponse quote(QuoteRequest request) {
        if (maximumDistanceKm != null && request.distanceKm() > maximumDistanceKm) {
            throw new ShippingException(
                    code + "_DISTANCE_EXCEEDED",
                    name + " delivery is available only for distances up to " + maximumDistanceKm + " km.",
                    422);
        }
        BigDecimal weightCharge = ratePerKg.multiply(request.weightKg()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceCharge = ratePerKm.multiply(BigDecimal.valueOf(request.distanceKm()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = baseFee.add(weightCharge).add(distanceCharge).setScale(2, RoundingMode.HALF_UP);
        return new QuoteResponse(code, deliveryEstimate, baseFee, weightCharge, distanceCharge, total);
    }
}

@Component
class StandardRateStrategy extends FixedRateStrategy {
    StandardRateStrategy() {
        super("STANDARD", "Standard", "3-5 business days", null, "5.00", "1.20", "0.05");
    }
}

@Component
class ExpressRateStrategy extends FixedRateStrategy {
    ExpressRateStrategy() {
        super("EXPRESS", "Express", "Next business day", null, "15.00", "2.50", "0.12");
    }
}

@Component
class SameDayRateStrategy extends FixedRateStrategy {
    SameDayRateStrategy() {
        super("SAME_DAY", "Same-Day", "Within 4 hours", 100, "35.00", "4.00", "0.25");
    }
}
