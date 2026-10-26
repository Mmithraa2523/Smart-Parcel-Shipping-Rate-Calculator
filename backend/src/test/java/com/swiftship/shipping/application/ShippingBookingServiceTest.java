package com.swiftship.shipping.application;

import com.swiftship.shipping.api.ApiModels.BookingRequest;
import com.swiftship.shipping.domain.ShippingException;
import com.swiftship.shipping.domain.ShippingPorts.BookingRecord;
import com.swiftship.shipping.domain.ShippingPorts.BookingStore;
import com.swiftship.shipping.domain.ShippingPorts.PaymentProcessor;
import com.swiftship.shipping.domain.ShippingPorts.PaymentResult;
import com.swiftship.shipping.domain.ShippingRateStrategy;
import com.swiftship.shipping.api.ApiModels.QuoteRequest;
import com.swiftship.shipping.api.ApiModels.QuoteResponse;
import com.swiftship.shipping.api.ApiModels.TierResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingBookingServiceTest {
    @Test
    void persistsOnlyApprovedBookingsAndSupportsReplacingPaymentAdapter() {
        ShippingRateStrategy strategy = mock(ShippingRateStrategy.class);
        when(strategy.code()).thenReturn("STANDARD");
        when(strategy.details()).thenReturn(new TierResponse(
                "STANDARD", "Standard", "3-5 business days",
                new BigDecimal("5.00"), new BigDecimal("1.20"), new BigDecimal("0.05"), null));
        when(strategy.quote(org.mockito.ArgumentMatchers.any(QuoteRequest.class)))
                .thenReturn(new QuoteResponse("STANDARD", "3-5 business days",
                        new BigDecimal("5.00"), new BigDecimal("1.20"), new BigDecimal("0.05"),
                        new BigDecimal("6.25")));
        ShippingPricingService pricing = new ShippingPricingService(List.of(strategy));

        BookingStore store = mock(BookingStore.class);
        PaymentProcessor payment = mock(PaymentProcessor.class);
        AtomicInteger persistenceAttempts = new AtomicInteger();
        when(store.save(org.mockito.ArgumentMatchers.any(BookingRecord.class))).thenAnswer(invocation -> {
            persistenceAttempts.incrementAndGet();
            return invocation.getArgument(0);
        });

        ShippingBookingService bookings = new ShippingBookingService(pricing, payment, store);
        when(payment.simulatePayment("CREDIT_CARD", new BigDecimal("6.25"), false))
                .thenReturn(new PaymentResult(true, "TEST-APPROVED", "Approved."));

        var booking = bookings.book(new BookingRequest(new BigDecimal("1"), 1,
                "STANDARD", "CREDIT_CARD", false));
        assertEquals("CONFIRMED", booking.status());
        assertEquals("TEST-APPROVED", booking.paymentReference());
        assertEquals(1, persistenceAttempts.get());
        verify(store).save(org.mockito.ArgumentMatchers.any(BookingRecord.class));

        when(payment.simulatePayment("CREDIT_CARD", new BigDecimal("6.25"), true))
                .thenReturn(new PaymentResult(false, null, "Simulated payment declined."));
        ShippingException declined = assertThrows(ShippingException.class,
                () -> bookings.book(new BookingRequest(new BigDecimal("1"), 1,
                        "STANDARD", "CREDIT_CARD", true)));
        assertEquals("PAYMENT_DECLINED", declined.code());
        assertEquals(402, declined.status());
        assertEquals(1, persistenceAttempts.get(), "Declined payments must never be persisted.");
    }

    @Test
    void rejectsUnsupportedPaymentMethodsBeforeCharging() {
        ShippingRateStrategy strategy = mock(ShippingRateStrategy.class);
        when(strategy.code()).thenReturn("STANDARD");
        PaymentProcessor payment = mock(PaymentProcessor.class);
        BookingStore store = mock(BookingStore.class);
        ShippingBookingService service = new ShippingBookingService(
                new ShippingPricingService(List.of(strategy)), payment, store);
        ShippingException exception = assertThrows(ShippingException.class,
                () -> service.book(new BookingRequest(new BigDecimal("1"), 1,
                        "STANDARD", "CRYPTO", false)));
        assertEquals("UNSUPPORTED_PAYMENT_METHOD", exception.code());
    }
}
