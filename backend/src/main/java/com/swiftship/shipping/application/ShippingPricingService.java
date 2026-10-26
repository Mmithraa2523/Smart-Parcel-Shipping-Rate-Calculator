package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.api.ApiModels.TierResponse;
import com.swiftship.shipping.domain.ShippingException;
import com.swiftship.shipping.domain.ShippingRateStrategy;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
public class ShippingPricingService implements ShippingQuoteCalculator {
    private final Map<String, ShippingRateStrategy> strategies;

    public ShippingPricingService(List<ShippingRateStrategy> strategies) {
        Map<String, ShippingRateStrategy> discovered = new java.util.LinkedHashMap<>();
        for (ShippingRateStrategy strategy : strategies) {
            String code = strategy.code().toUpperCase(Locale.ROOT);
            ShippingRateStrategy existing = discovered.putIfAbsent(code, strategy);
            if (existing != null) {
                throw new IllegalStateException("More than one rate strategy is registered for " + code);
            }
        }
        this.strategies = Map.copyOf(discovered);
    }

    public List<TierResponse> listTiers() {
        return strategies.values().stream()
                .sorted(Comparator.comparing(ShippingRateStrategy::code))
                .map(ShippingRateStrategy::details)
                .toList();
    }

    @Override
    public QuoteResponse quote(QuoteRequest request) {
        ShippingRateStrategy strategy = strategies.get(request.tier().toUpperCase(Locale.ROOT));
        if (strategy == null) {
            throw unsupportedTier(request.tier());
        }
        return strategy.quote(request);
    }

    private ShippingException unsupportedTier(String tier) {
        return new ShippingException("UNSUPPORTED_TIER",
                "Unsupported shipping tier '" + tier + "'. Available tiers: "
                        + String.join(", ", strategies.keySet()) + ".",
                422);
    }
}
