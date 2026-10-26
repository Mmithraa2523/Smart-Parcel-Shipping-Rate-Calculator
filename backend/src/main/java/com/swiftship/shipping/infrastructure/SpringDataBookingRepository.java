package com.swiftship.shipping.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBookingRepository extends JpaRepository<BookingEntity, String> {
}
