package com.vnn.ticket.model.enums;

public enum BookingStatus {
    HELD,       // Đang tạm giữ chỗ (thường trong 10 phút) chờ khách thanh toán
    CONFIRMED,  // Đã thanh toán thành công, xuất vé chính thức
    EXPIRED,    // Quá 10 phút không thanh toán, hệ thống tự động nhả vé lại vào kho
    CANCELLED   // Đơn đặt vé bị hủy
}
