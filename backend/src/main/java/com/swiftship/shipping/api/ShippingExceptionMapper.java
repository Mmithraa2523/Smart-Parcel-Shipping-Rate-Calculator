package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.ApiError;
import com.swiftship.shipping.domain.ShippingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

@Provider
@Component
public class ShippingExceptionMapper implements ExceptionMapper<ShippingException> {
    @Override
    public Response toResponse(ShippingException exception) {
        return Response.status(exception.status())
                .entity(new ApiError(exception.code(), exception.getMessage(), null))
                .build();
    }
}
