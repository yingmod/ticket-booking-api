package com.vnn.ticket.scheduler;

import com.vnn.ticket.model.Booking;
import com.vnn.ticket.model.TicketType;
import com.vnn.ticket.model.enums.BookingStatus;
import com.vnn.ticket.repository.BookingRepository;
import com.vnn.ticket.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSweeperScheduler {

    private final BookingRepository bookingRepository;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Chạy định kỳ mỗi 30 giây (30000ms) để quét và nhả các vé giữ chỗ (HELD) quá 10 phút không thanh toán
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void sweepExpiredHoldBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.HELD, now);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("🧹 [AUTO SWEEPER] Phát hiện {} đơn đặt vé giữ chỗ đã quá hạn 10 phút. Bắt đầu thu hồi và hoàn trả vé vào kho...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            TicketType ticketType = booking.getTicketType();
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + booking.getQuantity());
            ticketTypeRepository.save(ticketType);

            log.info("🔄 [HOÀN VÉ THÀNH CÔNG] Đơn {} đã hết hạn -> Trả lại {} vé cho hạng vé '{}' (Tồn kho mới: {})",
                    booking.getBookingCode(),
                    booking.getQuantity(),
                    ticketType.getName(),
                    ticketType.getAvailableQuantity());
        }
    }
}
