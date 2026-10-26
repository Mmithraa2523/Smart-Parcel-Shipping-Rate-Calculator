package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.BookingRequest;
import com.swiftship.shipping.api.ApiModels.BookingResponse;
import com.swiftship.shipping.application.ShippingBookingService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

@Component
@Path("/bookings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {
    private final ShippingBookingService bookingService;

    public BookingResource(ShippingBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @POST
    public Response book(@Valid BookingRequest request) {
        BookingResponse booking = bookingService.book(request);
        return Response.status(Response.Status.CREATED).entity(booking)
                .header("Location", "/api/bookings/" + booking.bookingId())
                .build();
    }
}
