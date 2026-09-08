package com.vnn.ticket.service.impl;

import com.vnn.ticket.dto.request.EventRequestDto;
import com.vnn.ticket.dto.request.TicketTypeRequestDto;
import com.vnn.ticket.dto.response.EventResponseDto;
import com.vnn.ticket.dto.response.TicketTypeResponseDto;
import com.vnn.ticket.exception.BadRequestException;
import com.vnn.ticket.exception.ResourceNotFoundException;
import com.vnn.ticket.model.Event;
import com.vnn.ticket.model.TicketType;
import com.vnn.ticket.model.enums.EventStatus;
import com.vnn.ticket.repository.EventRepository;
import com.vnn.ticket.repository.TicketTypeRepository;
import com.vnn.ticket.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    @Override
    @Transactional
    public EventResponseDto createEvent(EventRequestDto request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.UPCOMING)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Đã tạo sự kiện mới: id={}, name={}", saved.getId(), saved.getName());
        return mapToEventDto(saved);
    }

    @Override
    @Transactional
    public TicketTypeResponseDto createTicketType(TicketTypeRequestDto request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Sự kiện", "id", request.getEventId()));

        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity())
                .build();

        TicketType saved = ticketTypeRepository.save(ticketType);
        log.info("Đã tạo hạng vé mới: id={}, name={}, tổng số vé={}", saved.getId(), saved.getName(), saved.getTotalQuantity());
        return mapToTicketTypeDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDto> getAllEvents(EventStatus status) {
        List<Event> events = (status != null)
                ? eventRepository.findByStatus(status)
                : eventRepository.findAll();

        return events.stream().map(this::mapToEventDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sự kiện", "id", id));
        return mapToEventDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponseDto> getTicketTypesByEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Sự kiện", "id", eventId);
        }
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(this::mapToTicketTypeDto)
                .collect(Collectors.toList());
    }

    private EventResponseDto mapToEventDto(Event event) {
        List<TicketTypeResponseDto> typeDtos = event.getTicketTypes().stream()
                .map(this::mapToTicketTypeDto)
                .collect(Collectors.toList());

        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .ticketTypes(typeDtos)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private TicketTypeResponseDto mapToTicketTypeDto(TicketType type) {
        return TicketTypeResponseDto.builder()
                .id(type.getId())
                .eventId(type.getEvent().getId())
                .eventName(type.getEvent().getName())
                .name(type.getName())
                .description(type.getDescription())
                .price(type.getPrice())
                .totalQuantity(type.getTotalQuantity())
                .availableQuantity(type.getAvailableQuantity())
                .build();
    }
}
