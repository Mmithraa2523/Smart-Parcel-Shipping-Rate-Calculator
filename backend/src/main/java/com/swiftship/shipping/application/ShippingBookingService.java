package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.BookingRequest;
import com.swiftship.shipping.api.ApiModels.BookingResponse;
import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.domain.ShippingException;
import com.swiftship.shipping.domain.ShippingPorts.BookingRecord;
import com.swiftship.shipping.domain.ShippingPorts.BookingStore;
import com.swiftship.shipping.domain.ShippingPorts.PaymentProcessor;
import com.swiftship.shipping.domain.ShippingPorts.PaymentResult;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ShippingBookingService {
    private static final Set<String> PAYMENT_METHODS =
            Set.of("CREDIT_CARD", "CORPORATE_ACCOUNT_BILLING");

    private final ShippingQuoteCalculator pricingService;
    private final PaymentProcessor paymentProcessor;
    private final BookingStore bookingStore;

    public ShippingBookingService(
            ShippingQuoteCalculator pricingService,
            PaymentProcessor paymentProcessor,
            BookingStore bookingStore) {
        this.pricingService = pricingService;
        this.paymentProcessor = paymentProcessor;
        this.bookingStore = bookingStore;
    }

    public BookingResponse book(BookingRequest request) {
        String paymentMethod = request.paymentMethod().toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new ShippingException("UNSUPPORTED_PAYMENT_METHOD",
                    "Payment method must be CREDIT_CARD or CORPORATE_ACCOUNT_BILLING.",
                    422);
        }

        QuoteResponse quote = pricingService.quote(
                new QuoteRequest(request.weightKg(), request.distanceKm(), request.tier()));
        PaymentResult payment = paymentProcessor.simulatePayment(
                paymentMethod, quote.total(), Boolean.TRUE.equals(request.simulatePaymentFailure()));
        if (!payment.approved()) {
            throw new ShippingException("PAYMENT_DECLINED", payment.message(),
                    Response.Status.PAYMENT_REQUIRED.getStatusCode());
        }

        String bookingId = UUID.randomUUID().toString();
        BookingRecord saved = bookingStore.save(new BookingRecord(
                bookingId, "CONFIRMED", quote.tier(), quote.deliveryEstimate(),
                request.weightKg(), request.distanceKm(), quote.total(),
                paymentMethod, payment.reference()));
        return toResponse(saved);
    }

    private static BookingResponse toResponse(BookingRecord booking) {
        return new BookingResponse(booking.bookingId(), booking.status(), booking.tier(),
                booking.deliveryEstimate(), booking.total(), booking.paymentMethod(), booking.paymentReference());
    }
}
