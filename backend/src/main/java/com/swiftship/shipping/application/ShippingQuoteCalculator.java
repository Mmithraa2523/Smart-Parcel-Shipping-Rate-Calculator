package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;

public interface ShippingQuoteCalculator {
    QuoteResponse quote(QuoteRequest request);
}
