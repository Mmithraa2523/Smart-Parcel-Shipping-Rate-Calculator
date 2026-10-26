package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.BookingDetailsResponse;
import com.swiftship.shipping.domain.ShippingException;
import com.swiftship.shipping.domain.ShippingPorts.BookingRecord;
import com.swiftship.shipping.domain.ShippingPorts.BookingStore;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Service;

@Service
public class ShippingBookingQueryService {
    private final BookingStore bookingStore;

    public ShippingBookingQueryService(BookingStore bookingStore) {
        this.bookingStore = bookingStore;
    }

    public BookingDetailsResponse findById(String bookingId) {
        BookingRecord booking = bookingStore.findById(bookingId)
                .orElseThrow(() -> new ShippingException("BOOKING_NOT_FOUND",
                        "No booking was found for id '" + bookingId + "'.",
                        Response.Status.NOT_FOUND.getStatusCode()));
        return new BookingDetailsResponse(booking.bookingId(), booking.status(), booking.tier(),
                booking.deliveryEstimate(), booking.weightKg(), booking.distanceKm(),
                booking.total(), booking.paymentMethod(), booking.paymentReference());
    }
}
