package com.vnn.ticket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeRequestDto {

    @NotNull(message = "ID sự kiện không được để trống")
    private Long eventId;

    @NotBlank(message = "Tên hạng vé không được để trống")
    @Size(max = 100, message = "Tên hạng vé tối đa 100 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Tổng số lượng vé không được để trống")
    @Min(value = 1, message = "Tổng số vé phải từ 1 vé trở lên")
    private Integer totalQuantity;
}
