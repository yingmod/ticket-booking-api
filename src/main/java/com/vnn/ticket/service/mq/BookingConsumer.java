package com.vnn.ticket.service.mq;

import com.vnn.ticket.config.RabbitMQConfig;
import com.vnn.ticket.dto.message.BookingMessageDto;
import com.vnn.ticket.exception.ResourceNotFoundException;
import com.vnn.ticket.model.Booking;
import com.vnn.ticket.model.TicketType;
import com.vnn.ticket.model.User;
import com.vnn.ticket.model.enums.BookingStatus;
import com.vnn.ticket.repository.BookingRepository;
import com.vnn.ticket.repository.TicketTypeRepository;
import com.vnn.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConsumer {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;

    @Value("${ticket.reservation.hold-duration-minutes:10}")
    private int holdDurationMinutes;

    /**
     * Nhận tin nhắn từ hàng đợi RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleRabbitMQMessage(BookingMessageDto message) {
        log.info("📥 [RABBITMQ CONSUMER] Bốc tin nhắn từ hàng đợi RabbitMQ: bookingCode={}", message.getBookingCode());
        processBookingMessage(message);
    }

    /**
     * Lắng nghe sự kiện fallback nếu RabbitMQ không bật
     */
    @Async
    @EventListener
    public void handleLocalEvent(BookingMessageDto message) {
        log.info("📥 [LOCAL WORKER] Xử lý đơn đặt vé qua luồng ngầm nội bộ: bookingCode={}", message.getBookingCode());
        processBookingMessage(message);
    }

    @Transactional
    public void processBookingMessage(BookingMessageDto message) {
        // Kiểm tra xem mã booking đã được tạo chưa (Idempotency - chống xử lý lặp)
        if (bookingRepository.findByBookingCode(message.getBookingCode()).isPresent()) {
            log.warn("Đơn vé {} đã tồn tại trong DB, bỏ qua để tránh trùng lặp.", message.getBookingCode());
            return;
        }

        User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", message.getUserId()));

        TicketType ticketType = ticketTypeRepository.findById(message.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Hạng vé", "id", message.getTicketTypeId()));

        // Trừ số lượng vé khả dụng trong DB
        ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - message.getQuantity());
        ticketTypeRepository.save(ticketType);

        BigDecimal totalPrice = ticketType.getPrice().multiply(BigDecimal.valueOf(message.getQuantity()));
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);

        // Tạo đơn đặt vé ở trạng thái HELD (Giữ chỗ 10 phút chờ thanh toán)
        Booking booking = Booking.builder()
                .bookingCode(message.getBookingCode())
                .user(user)
                .ticketType(ticketType)
                .quantity(message.getQuantity())
                .totalPrice(totalPrice)
                .status(BookingStatus.HELD)
                .holdExpiresAt(holdExpiresAt)
                .build();

        bookingRepository.save(booking);

        log.info("🎉 [WORKER HOÀN TẤT] Đã tạo thành công đơn vé '{}' (Trạng thái: HELD - Giữ chỗ tới {})",
                booking.getBookingCode(), holdExpiresAt);
    }
}
