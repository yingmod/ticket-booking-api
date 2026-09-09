package com.vnn.ticket.controller;

import com.vnn.ticket.dto.request.BookTicketRequestDto;
import com.vnn.ticket.dto.response.ApiResponse;
import com.vnn.ticket.dto.response.BookingResponseDto;
import com.vnn.ticket.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "3. Ticket Bookings", description = "APIs săn vé tải cao (Redis Distributed Lock + RabbitMQ)")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Săn vé Flash Sale (Khóa phân tán Redis Redisson + Xếp hàng RabbitMQ)")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDto>> bookTicket(
            Principal principal,
            @Valid @RequestBody BookTicketRequestDto request
    ) {
        BookingResponseDto response = bookingService.bookTicket(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Yêu cầu đặt vé đã được tiếp nhận và xử lý!"));
    }

    @Operation(summary = "Xác nhận thanh toán cho đơn đặt vé đang giữ chỗ (HELD -> CONFIRMED)")
    @PostMapping("/{bookingCode}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingResponseDto>> confirmPayment(
            Principal principal,
            @PathVariable String bookingCode
    ) {
        BookingResponseDto response = bookingService.confirmPayment(principal.getName(), bookingCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận thanh toán thành công!"));
    }

    @Operation(summary = "Hủy đơn đặt vé (Tự động hoàn trả vé lại vào kho)")
    @PostMapping("/{bookingCode}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDto>> cancelBooking(
            Principal principal,
            @PathVariable String bookingCode
    ) {
        BookingResponseDto response = bookingService.cancelBooking(principal.getName(), bookingCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn đặt vé thành công!"));
    }

    @Operation(summary = "Xem danh sách các vé đã đặt của tài khoản hiện tại")
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(Principal principal) {
        List<BookingResponseDto> response = bookingService.getMyBookings(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách vé thành công"));
    }

    @Operation(summary = "Tra cứu chi tiết đơn đặt vé theo mã bookingCode")
    @GetMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponseDto>> getBookingByCode(@PathVariable String bookingCode) {
        BookingResponseDto response = bookingService.getBookingByCode(bookingCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu đơn đặt vé thành công"));
    }
}
