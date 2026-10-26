package com.swiftship.shipping.application;

import com.swiftship.shipping.domain.ShippingPorts.PaymentProcessor;
import com.swiftship.shipping.domain.ShippingPorts.PaymentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult simulatePayment(String paymentMethod, BigDecimal amount, boolean simulateFailure) {
        if (simulateFailure) {
            return new PaymentResult(false, null, "Simulated payment declined. No booking was created.");
        }
        return new PaymentResult(true, "SIM-" + UUID.randomUUID(), "Payment approved.");
    }
}
