package com.vnn.ticket.service.mq;

import com.vnn.ticket.config.RabbitMQConfig;
import com.vnn.ticket.dto.message.BookingMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public void sendBookingMessage(BookingMessageDto message) {
        try {
            log.info("📨 [RABBITMQ PRODUCER] Bắn thông điệp đặt vé vào hàng đợi: bookingCode={}, userId={}, ticketTypeId={}, quantity={}",
                    message.getBookingCode(), message.getUserId(), message.getTicketTypeId(), message.getQuantity());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    message
            );
            log.info("✅ [RABBITMQ PRODUCER] Gửi tin nhắn vào RabbitMQ thành công!");
        } catch (Exception ex) {
            log.warn("⚠️ [RABBITMQ FALLBACK] Không thể kết nối tới RabbitMQ Broker ('{}'). Chuyển sang phát Event ngầm nội bộ Spring EventPublisher!", ex.getMessage());
            // Phát sự kiện nội bộ để Worker xử lý ngay lập tức nếu chưa bật container RabbitMQ
            eventPublisher.publishEvent(message);
        }
    }
}
