package com.vnn.ticket.dto.response;

import com.vnn.ticket.model.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

    private Long id;
    private String bookingCode;
    private Long userId;
    private String username;
    private Long eventId;
    private String eventName;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime createdAt;
    private String message;
}
