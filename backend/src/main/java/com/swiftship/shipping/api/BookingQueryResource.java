package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.BookingDetailsResponse;
import com.swiftship.shipping.application.ShippingBookingQueryService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

@Component
@Path("/bookings/{bookingId}")
@Produces(MediaType.APPLICATION_JSON)
public class BookingQueryResource {
    private final ShippingBookingQueryService bookingQueryService;

    public BookingQueryResource(ShippingBookingQueryService bookingQueryService) {
        this.bookingQueryService = bookingQueryService;
    }

    @GET
    public BookingDetailsResponse getBooking(@PathParam("bookingId") String bookingId) {
        return bookingQueryService.findById(bookingId);
    }
}
