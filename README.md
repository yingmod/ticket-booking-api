# High-Concurrency Flash Sale & Ticket Booking System 🎟️

[![CI/CD Pipeline](https://github.com/yingmod/ticket-booking-api/actions/workflows/ci.yml/badge.svg)](https://github.com/yingmod/ticket-booking-api/actions/workflows/ci.yml)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-Swagger%20UI-brightgreen?logo=swagger)](https://ticket-booking-api-m0lv.onrender.com/swagger-ui.html)

> 🌐 **Live Demo (Trải nghiệm trực tuyến 24/7):** [https://ticket-booking-api-m0lv.onrender.com/swagger-ui.html](https://ticket-booking-api-m0lv.onrender.com/swagger-ui.html)

Dự án thứ 5 - Dự án đỉnh cao (Capstone Project) trong lộ trình thực chiến Backend Spring Boot chuyên sâu. Hệ thống mô phỏng nền tảng săn vé Concert / Flash Sale tải cao với kiến trúc **Redis Distributed Lock (Redisson)** và **RabbitMQ Event-Driven Architecture**.

---

## 🚀 Công Nghệ Sử Dụng

- **Java 21 LTS** & **Spring Boot 3.3.4**
- **Spring Security 6** & **JWT (JSON Web Token)**
- **Spring Data JPA** & **Hibernate 6**
- **H2 In-Memory Database** & H2 Web Console
- **Redis & Redisson (3.34.1)**: Khóa phân tán (Distributed Lock) siêu tốc độ trên RAM chống bán âm/vượt quá số lượng vé khi có 50.000 requests/s
- **RabbitMQ**: Hàng đợi thông điệp xếp hàng xử lý đặt vé bất đồng bộ (Async Queue) chống quá tải DB
- **Spring Scheduling**: Bộ quét ngầm tự động nhả vé quá hạn giữ chỗ 10 phút (`@Scheduled`)
- **Docker & Docker Compose**: Khởi tạo cụm Redis & RabbitMQ trong 1 câu lệnh
- **Springdoc OpenAPI / Swagger UI** (v2.5.0)
- **Lombok** & **Jakarta Validation**

---

## 🔑 Điểm Nhấn Kiến Trúc & Kỹ Thuật Đỉnh Cao

### 1. Khóa phân tán Redisson (Redis Distributed Lock)
- **Bài toán:** Khi mở bán vé Concert (như BlackPink hay Anh Trai Say Hi), có 50.000 người cùng bấm "Mua vé" trong 1 giây. Nếu dùng Khóa cơ sở dữ liệu (`SELECT ... FOR UPDATE`), Database sẽ bị nghẽn Connection Pool và sập ngay lập tức.
- **Giải pháp:** Sử dụng **Redisson Distributed Lock** trên RAM của Redis:
  ```java
  RLock lock = redissonClient.getLock("lock:ticket:type:" + ticketTypeId);
  boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
  ```
  Tốc độ xử lý khóa trên RAM dưới **1ms**. Chỉ những request giành được khóa mới được tiến hành giữ vé, loại bỏ 100% rủi ro bán vượt quá số vé phát hành.

### 2. Kiến trúc Hàng đợi Bất đồng bộ (Event-Driven with RabbitMQ)
- API tiếp nhận yêu cầu đặt vé của khách hàng, đóng gói thông điệp `BookingMessageDto` rồi đẩy vào hàng đợi `ticket.booking.queue` qua bộ chuyển tiếp `ticket.booking.exchange` và trả về ngay mã trạng thái `202 Accepted`.
- Phía sau, **Worker (`BookingConsumer`)** nhặt từng tin nhắn ra xử lý trừ vé và lưu đơn đặt chỗ vào DB theo tốc độ ổn định mà không gây nghẽn hệ thống.

### 3. Cơ chế Giữ vé 10 phút & Tự động hoàn kho (Holding Seat with Auto Sweeper)
- Khách hàng săn được vé sẽ có **10 phút** để thanh toán (trạng thái `HELD`).
- Tác vụ ngầm [`TicketSweeperScheduler.java`](file:///Users/macbook/Workspace/Personal/ticket-booking-api/src/main/java/com/vnn/ticket/scheduler/TicketSweeperScheduler.java) chạy định kỳ mỗi 30 giây (`@Scheduled(fixedRate = 30000)`), tự động quét các đơn quá hạn 10 phút để hủy đơn (`EXPIRED`) và hoàn trả vé lại vào kho cho người khác mua.

---

## 📋 Danh Sách API Endpoints (Swagger OpenAPI)

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:ticketdb`)

| Method | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/register` | Public | Đăng ký tài khoản người dùng mới |
| **POST** | `/api/v1/auth/login` | Public | Đăng nhập nhận JWT Bearer Token |
| **GET** | `/api/v1/events` | Public | Lấy danh sách sự kiện/Concert |
| **GET** | `/api/v1/events/{id}` | Public | Lấy chi tiết sự kiện |
| **GET** | `/api/v1/events/{id}/ticket-types` | Public | Xem các hạng vé (VVIP, VIP, GA) & số vé còn lại |
| **POST** | `/api/v1/events` | `ADMIN` | Tạo mới sự kiện/Concert |
| **POST** | `/api/v1/events/ticket-types` | `ADMIN` | Mở thêm hạng vé cho sự kiện |
| **POST** | `/api/v1/bookings` | `CUSTOMER` | **Săn vé Flash Sale (Khóa Redis + RabbitMQ)** |
| **POST** | `/api/v1/bookings/{code}/confirm-payment` | `CUSTOMER` | Xác nhận thanh toán vé (HELD -> CONFIRMED) |
| **POST** | `/api/v1/bookings/{code}/cancel` | `CUSTOMER` | Hủy vé (hoàn trả số lượng vé lại vào kho) |
| **GET** | `/api/v1/bookings/my-bookings` | `CUSTOMER` | Xem danh sách vé đã đặt của bản thân |
| **GET** | `/api/v1/bookings/{code}` | `CUSTOMER` | Tra cứu chi tiết đơn đặt vé |

---

## 👥 Dữ Liệu Mẫu Định Sẵn (DataInitializer)

Hệ thống tự động khởi tạo dữ liệu mẫu khi khởi động:
- **Tài khoản:**
  - Admin: `admin` / `admin123`
  - Khách hàng 1: `customer1` / `123456`
  - Khách hàng 2: `customer2` / `123456`
- **Sự kiện mẫu:**
  - **Concert Anh Trai Say Hi 2026 - SVĐ Quốc Gia Mỹ Đình** (Trạng thái: `ACTIVE`)
- **Hạng vé:**
  - `VVIP Lounge`: Giá 10.000.000 VNĐ (Tổng: **10 vé** - Flash sale số lượng giới hạn)
  - `VIP 1`: Giá 4.500.000 VNĐ (Tổng: **50 vé**)
  - `GA Standing`: Giá 1.800.000 VNĐ (Tổng: **200 vé**)

---

## 🛠️ Hướng Dẫn Chạy & Kiểm Thử

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)
Chỉ với 1 câu lệnh duy nhất, toàn bộ ứng dụng (Spring Boot App + Redis 7.2 + RabbitMQ 3.13) sẽ được build và khởi chạy độc lập trong Docker container:

```bash
docker compose up -d --build
```
- **Ứng dụng:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **RabbitMQ Management Dashboard:** [http://localhost:15672](http://localhost:15672) (User: `guest` / Pass: `guest`)
- **Dừng hệ thống:** `docker compose down`

### Cách 2: Chạy môi trường Local Development
1. Khởi động các dịch vụ phụ trợ (Redis & RabbitMQ):
   ```bash
   docker compose up -d redis rabbitmq
   ```
2. Khởi chạy ứng dụng Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Truy cập Swagger UI trải nghiệm:
   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🔄 Quy Trình CI/CD (GitHub Actions)

Dự án tích hợp đường ống CI/CD tự động (`.github/workflows/ci.yml`):
- **Tự động kích hoạt** mỗi khi có `push` hoặc `pull_request` vào nhánh `main`.
- **Dựng môi trường kiểm thử thực tế**: Tự động spin-up Service Containers cho Redis & RabbitMQ trên GitHub Runner.
- **Kiểm thử tự động**: Chạy toàn bộ test suites (`./mvnw clean test`).
- **Đóng gói Docker**: Build kiểm tra Multi-stage Docker Image (`Dockerfile`) tối ưu kích thước (~140MB JRE Alpine) chạy dưới quyền Non-root user (`appuser`).

