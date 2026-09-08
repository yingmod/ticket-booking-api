package com.vnn.ticket.dto.response;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeResponseDto implements Serializable {

    private Long id;
    private Long eventId;
    private String eventName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
}
