package com.vnn.ticket.service;

import com.vnn.ticket.dto.request.EventRequestDto;
import com.vnn.ticket.dto.request.TicketTypeRequestDto;
import com.vnn.ticket.dto.response.EventResponseDto;
import com.vnn.ticket.dto.response.TicketTypeResponseDto;
import com.vnn.ticket.model.enums.EventStatus;

import java.util.List;

public interface EventService {

    EventResponseDto createEvent(EventRequestDto request);

    TicketTypeResponseDto createTicketType(TicketTypeRequestDto request);

    List<EventResponseDto> getAllEvents(EventStatus status);

    EventResponseDto getEventById(Long id);

    List<TicketTypeResponseDto> getTicketTypesByEvent(Long eventId);
}
