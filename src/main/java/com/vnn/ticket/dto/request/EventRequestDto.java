package com.vnn.ticket.dto.request;

import com.vnn.ticket.model.enums.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestDto {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = 200, message = "Tên sự kiện tối đa 200 ký tự")
    private String name;

    private String description;

    @NotBlank(message = "Địa điểm tổ chức không được để trống")
    @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
    private String venue;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    private EventStatus status;
}
