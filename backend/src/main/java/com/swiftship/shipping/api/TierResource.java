package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.TierResponse;
import com.swiftship.shipping.application.ShippingPricingService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Path("/tiers")
@Produces(MediaType.APPLICATION_JSON)
public class TierResource {
    private final ShippingPricingService pricingService;

    public TierResource(ShippingPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GET
    public List<TierResponse> listTiers() {
        return pricingService.listTiers();
    }
}
