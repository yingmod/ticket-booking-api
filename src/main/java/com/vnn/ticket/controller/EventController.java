package com.vnn.ticket.controller;

import com.vnn.ticket.dto.request.EventRequestDto;
import com.vnn.ticket.dto.request.TicketTypeRequestDto;
import com.vnn.ticket.dto.response.ApiResponse;
import com.vnn.ticket.dto.response.EventResponseDto;
import com.vnn.ticket.dto.response.TicketTypeResponseDto;
import com.vnn.ticket.model.enums.EventStatus;
import com.vnn.ticket.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "2. Events & Concerts", description = "APIs xem và quản lý sự kiện, các hạng vé")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Lấy danh sách tất cả các sự kiện/Concert (Có thể lọc theo trạng thái)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> getAllEvents(
            @RequestParam(required = false) EventStatus status
    ) {
        List<EventResponseDto> events = eventService.getAllEvents(status);
        return ResponseEntity.ok(ApiResponse.success(events, "Lấy danh sách sự kiện thành công"));
    }

    @Operation(summary = "Lấy chi tiết sự kiện theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDto>> getEventById(@PathVariable Long id) {
        EventResponseDto event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success(event, "Lấy chi tiết sự kiện thành công"));
    }

    @Operation(summary = "Lấy danh sách các hạng vé (VVIP, VIP, GA) và số vé còn lại của sự kiện")
    @GetMapping("/{id}/ticket-types")
    public ResponseEntity<ApiResponse<List<TicketTypeResponseDto>>> getTicketTypes(@PathVariable Long id) {
        List<TicketTypeResponseDto> ticketTypes = eventService.getTicketTypesByEvent(id);
        return ResponseEntity.ok(ApiResponse.success(ticketTypes, "Lấy danh sách hạng vé thành công"));
    }

    @Operation(
            summary = "Tạo mới sự kiện/Concert (Dành riêng cho ADMIN)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(@Valid @RequestBody EventRequestDto request) {
        EventResponseDto response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo sự kiện thành công"));
    }

    @Operation(
            summary = "Mở bán thêm hạng vé cho sự kiện (Dành riêng cho ADMIN)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ticket-types")
    public ResponseEntity<ApiResponse<TicketTypeResponseDto>> createTicketType(@Valid @RequestBody TicketTypeRequestDto request) {
        TicketTypeResponseDto response = eventService.createTicketType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo hạng vé thành công"));
    }
}
