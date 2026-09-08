package com.vnn.ticket.dto.response;

import com.vnn.ticket.model.enums.EventStatus;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String venue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventStatus status;
    @Builder.Default
    private List<TicketTypeResponseDto> ticketTypes = new ArrayList<>();
    private LocalDateTime createdAt;
}
