package com.vnn.ticket.dto.message;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingMessageDto implements Serializable {

    private String bookingCode;
    private Long userId;
    private String username;
    private Long ticketTypeId;
    private Integer quantity;

    @Builder.Default
    private LocalDateTime requestTime = LocalDateTime.now();
}
