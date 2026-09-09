# ==========================================================
# GIAI ĐOẠN 1: BUILD STAGE (Dùng full JDK 21 để biên dịch mã nguồn)
# ==========================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Tối ưu hóa Docker Layer Caching:
# Copy cấu hình Maven trước, nếu file pom.xml không đổi thì Docker sẽ dùng lại cache, không tải lại thư viện!
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy toàn bộ mã nguồn vào và tiến hành đóng gói file .jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================================
# GIAI ĐOẠN 2: RUNTIME STAGE (Chỉ dùng JRE 21 Alpine siêu nhẹ ~140MB)
# ==========================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Chuẩn bảo mật doanh nghiệp: Tạo người dùng không có quyền root (Non-root user)
# Tránh trường hợp ứng dụng bị tấn công thì hacker không thể can thiệp vào máy chủ Linux
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Chỉ copy đúng 1 file .jar từ Giai đoạn 1 sang, bỏ lại toàn bộ mã nguồn và Maven thừa
COPY --from=builder /build/target/*.jar app.jar

# Phân quyền thư mục cho appuser
RUN chown -R appuser:appgroup /app

# Kích hoạt người dùng non-root
USER appuser

# Khai báo cổng ứng dụng
EXPOSE 8080

# Tối ưu hóa bộ nhớ JVM trong môi trường Docker Container:
# - UseContainerSupport: Nhận biết giới hạn CPU/RAM của Container thay vì nhìn RAM của cả máy chủ vật lý
# - MaxRAMPercentage=75.0: Cho phép JVM sử dụng tối đa 75% RAM được cấp phát cho Container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
