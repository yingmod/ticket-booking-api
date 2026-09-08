package com.vnn.ticket.repository;

import com.vnn.ticket.model.Booking;
import com.vnn.ticket.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Tìm các đơn đặt vé đang ở trạng thái HELD nhưng thời gian giữ chỗ đã quá hạn
    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime dateTime);
}
