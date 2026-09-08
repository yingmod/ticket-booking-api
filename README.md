# High-Concurrency Flash Sale & Ticket Booking System 🎟️

Dự án thứ 5 trong lộ trình thực chiến Backend Spring Boot chuyên sâu. Hệ thống mô phỏng nền tảng săn vé Concert / Flash Sale tải cao với kiến trúc **Redis Distributed Lock** và **RabbitMQ Event-Driven**.

---

## 🚀 Công Nghệ Sử Dụng
- **Java 21 LTS** & **Spring Boot 3.3.4**
- **Spring Security 6** & **JWT (JSON Web Token)**
- **Spring Data JPA** & **H2 Database**
- **Redis & Redisson**: Khóa phân tán (Distributed Lock) tốc độ cao chống bán vượt quá số lượng vé
- **RabbitMQ**: Message Broker xếp hàng xử lý đặt vé bất đồng bộ (Event-Driven Architecture)
- **Springdoc OpenAPI / Swagger UI** (v2.5.0)
- **Lombok** & **Jakarta Validation**
