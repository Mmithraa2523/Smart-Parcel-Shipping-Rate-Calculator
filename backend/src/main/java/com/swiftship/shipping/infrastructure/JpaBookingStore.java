package com.swiftship.shipping.infrastructure;

import com.swiftship.shipping.domain.ShippingPorts.BookingRecord;
import com.swiftship.shipping.domain.ShippingPorts.BookingStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaBookingStore implements BookingStore {
    private final SpringDataBookingRepository repository;

    public JpaBookingStore(SpringDataBookingRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookingRecord save(BookingRecord booking) {
        return repository.save(BookingEntity.from(booking)).toRecord();
    }

    @Override
    public Optional<BookingRecord> findById(String bookingId) {
        return repository.findById(bookingId).map(BookingEntity::toRecord);
    }
}
