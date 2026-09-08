package com.vnn.ticket.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookTicketRequestDto {

    @NotNull(message = "ID hạng vé không được để trống")
    private Long ticketTypeId;

    @NotNull(message = "Số lượng vé không được để trống")
    @Min(value = 1, message = "Số lượng vé tối thiểu là 1")
    @Max(value = 4, message = "Để chống phe vé/đầu cơ, mỗi lượt đặt tối đa 4 vé")
    private Integer quantity;
}
