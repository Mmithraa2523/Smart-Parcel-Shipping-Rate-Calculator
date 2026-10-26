package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.application.ShippingPricingService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

@Component
@Path("/quotes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuoteResource {
    private final ShippingPricingService pricingService;

    public QuoteResource(ShippingPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @POST
    public QuoteResponse quote(@Valid QuoteRequest request) {
        return pricingService.quote(request);
    }
}
