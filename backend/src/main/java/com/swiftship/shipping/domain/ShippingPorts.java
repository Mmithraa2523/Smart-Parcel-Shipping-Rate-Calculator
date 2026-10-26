package com.swiftship.shipping.domain;

import java.math.BigDecimal;
import java.util.Optional;

public final class ShippingPorts {
    private ShippingPorts() {
    }

    public interface PaymentProcessor {
        PaymentResult simulatePayment(String paymentMethod, BigDecimal amount, boolean simulateFailure);
    }

    public interface BookingStore {
        BookingRecord save(BookingRecord booking);

        Optional<BookingRecord> findById(String bookingId);
    }

    public record PaymentResult(boolean approved, String reference, String message) {
    }

    public record BookingRecord(
            String bookingId,
            String status,
            String tier,
            String deliveryEstimate,
            BigDecimal weightKg,
            Integer distanceKm,
            BigDecimal total,
            String paymentMethod,
            String paymentReference) {
    }
}
