package com.swiftship.shipping.infrastructure;

import com.swiftship.shipping.domain.ShippingPorts.BookingRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_bookings")
public class BookingEntity {
    @Id
    private String bookingId;
    private String status;
    private String tier;
    private String deliveryEstimate;
    private BigDecimal weightKg;
    private Integer distanceKm;
    private BigDecimal total;
    private String paymentMethod;
    private String paymentReference;

    protected BookingEntity() {
    }

    private BookingEntity(BookingRecord booking) {
        bookingId = booking.bookingId();
        status = booking.status();
        tier = booking.tier();
        deliveryEstimate = booking.deliveryEstimate();
        weightKg = booking.weightKg();
        distanceKm = booking.distanceKm();
        total = booking.total();
        paymentMethod = booking.paymentMethod();
        paymentReference = booking.paymentReference();
    }

    public static BookingEntity from(BookingRecord booking) {
        return new BookingEntity(booking);
    }

    public BookingRecord toRecord() {
        return new BookingRecord(bookingId, status, tier, deliveryEstimate,
                weightKg, distanceKm, total, paymentMethod, paymentReference);
    }
}
