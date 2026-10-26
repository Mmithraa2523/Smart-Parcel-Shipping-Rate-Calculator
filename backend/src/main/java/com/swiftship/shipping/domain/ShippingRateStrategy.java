package com.swiftship.shipping.domain;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.api.ApiModels.TierResponse;

public interface ShippingRateStrategy {
    String code();

    TierResponse details();

    QuoteResponse quote(QuoteRequest request);
}
