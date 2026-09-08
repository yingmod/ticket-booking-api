package com.vnn.ticket.service;

import com.vnn.ticket.dto.request.BookTicketRequestDto;
import com.vnn.ticket.dto.response.BookingResponseDto;

import java.util.List;

public interface BookingService {

    BookingResponseDto bookTicket(String username, BookTicketRequestDto request);

    BookingResponseDto confirmPayment(String username, String bookingCode);

    BookingResponseDto cancelBooking(String username, String bookingCode);

    List<BookingResponseDto> getMyBookings(String username);

    BookingResponseDto getBookingByCode(String bookingCode);
}
