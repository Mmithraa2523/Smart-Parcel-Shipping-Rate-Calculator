package com.swiftship.shipping.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record QuoteRequest(
            @NotNull(message = "Weight is required")
            @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
            @DecimalMax(value = "50.0", message = "Weight must not exceed 50.0 kg")
            BigDecimal weightKg,
            @NotNull(message = "Distance is required")
            @Min(value = 1, message = "Distance must be at least 1 km")
            @Max(value = 2500, message = "Distance must not exceed 2500 km")
            Integer distanceKm,
            @NotBlank(message = "A shipping tier is required")
            String tier) {
    }

    public record BookingRequest(
            @NotNull(message = "Weight is required")
            @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
            @DecimalMax(value = "50.0", message = "Weight must not exceed 50.0 kg")
            BigDecimal weightKg,
            @NotNull(message = "Distance is required")
            @Min(value = 1, message = "Distance must be at least 1 km")
            @Max(value = 2500, message = "Distance must not exceed 2500 km")
            Integer distanceKm,
            @NotBlank(message = "A shipping tier is required")
            String tier,
            @NotBlank(message = "A payment method is required")
            String paymentMethod,
            Boolean simulatePaymentFailure) {
    }

    public record TierResponse(
            String code,
            String name,
            String deliveryEstimate,
            BigDecimal baseFee,
            BigDecimal ratePerKg,
            BigDecimal ratePerKm,
            Integer maximumDistanceKm) {
    }

    public record QuoteResponse(
            String tier,
            String deliveryEstimate,
            BigDecimal baseFee,
            BigDecimal weightCharge,
            BigDecimal distanceCharge,
            BigDecimal total) {
    }

    public record BookingResponse(
            String bookingId,
            String status,
            String tier,
            String deliveryEstimate,
            BigDecimal total,
            String paymentMethod,
            String paymentReference) {
    }

    public record BookingDetailsResponse(
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ApiError(
            String code,
            String message,
            List<ValidationIssue> details) {
    }

    public record ValidationIssue(String field, String message) {
    }

    public record OpenApiDocument(String content) {
    }
}
