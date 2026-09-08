package com.vnn.ticket.service.impl;

import com.vnn.ticket.dto.message.BookingMessageDto;
import com.vnn.ticket.dto.request.BookTicketRequestDto;
import com.vnn.ticket.dto.response.BookingResponseDto;
import com.vnn.ticket.exception.BadRequestException;
import com.vnn.ticket.exception.ResourceNotFoundException;
import com.vnn.ticket.model.Booking;
import com.vnn.ticket.model.TicketType;
import com.vnn.ticket.model.User;
import com.vnn.ticket.model.enums.BookingStatus;
import com.vnn.ticket.model.enums.EventStatus;
import com.vnn.ticket.repository.BookingRepository;
import com.vnn.ticket.repository.TicketTypeRepository;
import com.vnn.ticket.repository.UserRepository;
import com.vnn.ticket.service.BookingService;
import com.vnn.ticket.service.TicketLockService;
import com.vnn.ticket.service.mq.RabbitMQSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketLockService ticketLockService;
    private final RabbitMQSender rabbitMQSender;

    @Value("${ticket.reservation.hold-duration-minutes:10}")
    private int holdDurationMinutes;

    @Override
    public BookingResponseDto bookTicket(String username, BookTicketRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "username", username));

        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Hạng vé", "id", request.getTicketTypeId()));

        if (ticketType.getEvent().getStatus() != EventStatus.ACTIVE) {
            throw new BadRequestException("Sự kiện '" + ticketType.getEvent().getName() + "' hiện chưa mở bán vé hoặc đã kết thúc");
        }

        // Bọc toàn bộ logic kiểm tra và giữ vé vào REDIS DISTRIBUTED LOCK
        return ticketLockService.executeWithLock(ticketType.getId(), () -> {
            TicketType currentTicket = ticketTypeRepository.findById(ticketType.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hạng vé", "id", ticketType.getId()));

            if (currentTicket.getAvailableQuantity() < request.getQuantity()) {
                throw new BadRequestException(String.format("Rất tiếc! Hạng vé '%s' không đủ số lượng (còn lại: %d, yêu cầu: %d)",
                        currentTicket.getName(), currentTicket.getAvailableQuantity(), request.getQuantity()));
            }

            // Sinh mã đặt chỗ duy nhất
            String bookingCode = "BK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

            // Đóng gói thông điệp và bắn vào RabbitMQ Queue
            BookingMessageDto message = BookingMessageDto.builder()
                    .bookingCode(bookingCode)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .ticketTypeId(currentTicket.getId())
                    .quantity(request.getQuantity())
                    .requestTime(LocalDateTime.now())
                    .build();

            rabbitMQSender.sendBookingMessage(message);

            BigDecimal totalPrice = currentTicket.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);

            log.info("Đặt vé thành công trong luồng Distributed Lock! Mã: {}, Khách: {}, Số vé: {}",
                    bookingCode, username, request.getQuantity());

            return BookingResponseDto.builder()
                    .bookingCode(bookingCode)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .eventId(currentTicket.getEvent().getId())
                    .eventName(currentTicket.getEvent().getName())
                    .ticketTypeId(currentTicket.getId())
                    .ticketTypeName(currentTicket.getName())
                    .quantity(request.getQuantity())
                    .unitPrice(currentTicket.getPrice())
                    .totalPrice(totalPrice)
                    .status(BookingStatus.HELD)
                    .holdExpiresAt(holdExpiresAt)
                    .createdAt(LocalDateTime.now())
                    .message(String.format("Vé của bạn đang được tạm giữ trong %d phút. Vui lòng thanh toán trước %s!",
                            holdDurationMinutes, holdExpiresAt.toLocalTime().toString().substring(0, 8)))
                    .build();
        });
    }

    @Override
    @Transactional
    public BookingResponseDto confirmPayment(String username, String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", "bookingCode", bookingCode));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Bạn không có quyền thanh toán cho đơn đặt vé này");
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            throw new BadRequestException("Đơn đặt vé không ở trạng thái chờ thanh toán (Trạng thái hiện tại: " + booking.getStatus() + ")");
        }

        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BadRequestException("Thời gian giữ vé 10 phút đã hết hạn! Vé đã được tự động hoàn lại kho.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(booking);

        log.info("Xác nhận thanh toán thành công cho đơn vé: {}", bookingCode);
        return mapToDto(updated, "Thanh toán thành công! Vé chính thức thuộc về bạn.");
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(String username, String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", "bookingCode", bookingCode));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn đặt vé này");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Vé đã thanh toán thành công, không thể tự hủy qua API này");
        }

        if (booking.getStatus() == BookingStatus.HELD) {
            // Hoàn lại số lượng vé khả dụng vào kho
            TicketType ticketType = booking.getTicketType();
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + booking.getQuantity());
            ticketTypeRepository.save(ticketType);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);

        log.info("Đã hủy đơn đặt vé: {}, hoàn lại {} vé vào kho", bookingCode, booking.getQuantity());
        return mapToDto(updated, "Hủy đơn đặt vé thành công. Vé đã được trả lại kho.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "username", username));

        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(b -> mapToDto(b, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", "bookingCode", bookingCode));
        return mapToDto(booking, null);
    }

    private BookingResponseDto mapToDto(Booking booking, String customMessage) {
        return BookingResponseDto.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .eventId(booking.getTicketType().getEvent().getId())
                .eventName(booking.getTicketType().getEvent().getName())
                .ticketTypeId(booking.getTicketType().getId())
                .ticketTypeName(booking.getTicketType().getName())
                .quantity(booking.getQuantity())
                .unitPrice(booking.getTicketType().getPrice())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .createdAt(booking.getCreatedAt())
                .message(customMessage)
                .build();
    }
}
