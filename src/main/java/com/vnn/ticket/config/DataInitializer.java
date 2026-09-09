package com.vnn.ticket.config;

import com.vnn.ticket.model.Event;
import com.vnn.ticket.model.Role;
import com.vnn.ticket.model.TicketType;
import com.vnn.ticket.model.User;
import com.vnn.ticket.model.enums.EventStatus;
import com.vnn.ticket.model.enums.RoleType;
import com.vnn.ticket.repository.EventRepository;
import com.vnn.ticket.repository.RoleRepository;
import com.vnn.ticket.repository.TicketTypeRepository;
import com.vnn.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("===> Khởi tạo dữ liệu mẫu cho Hệ thống Săn vé Concert Tải cao...");

        // 1. Khởi tạo Roles
        Role roleCustomer = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleType.ROLE_CUSTOMER)));

        Role roleAdmin = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleType.ROLE_ADMIN)));

        // 2. Khởi tạo Users
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@ticketmaster.vn")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản trị viên Ban tổ chức")
                    .phoneNumber("0909999999")
                    .roles(Set.of(roleAdmin, roleCustomer))
                    .build();
            userRepository.save(admin);
            log.info("Đã tạo tài khoản ADMIN: admin / admin123");
        }

        if (!userRepository.existsByUsername("customer1")) {
            User customer1 = User.builder()
                    .username("customer1")
                    .email("customer1@gmail.com")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Trần Thị Fan Cứng")
                    .phoneNumber("0912345678")
                    .roles(Set.of(roleCustomer))
                    .build();
            userRepository.save(customer1);
            log.info("Đã tạo tài khoản CUSTOMER 1: customer1 / 123456");
        }

        if (!userRepository.existsByUsername("customer2")) {
            User customer2 = User.builder()
                    .username("customer2")
                    .email("customer2@gmail.com")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Lê Văn Săn Vé")
                    .phoneNumber("0987654321")
                    .roles(Set.of(roleCustomer))
                    .build();
            userRepository.save(customer2);
            log.info("Đã tạo tài khoản CUSTOMER 2: customer2 / 123456");
        }

        // 3. Khởi tạo Sự kiện mẫu (Concert Anh Trai Say Hi)
        if (eventRepository.count() == 0) {
            Event concert = Event.builder()
                    .name("Concert Anh Trai Say Hi 2026 - SVĐ Quốc Gia Mỹ Đình")
                    .description("Đại nhạc hội quy tụ hơn 30 anh trai đình đám cùng hệ thống sân khấu, ánh sáng và âm thanh tiêu chuẩn quốc tế.")
                    .venue("Sân vận động Quốc gia Mỹ Đình, Đường Lê Đức Thọ, Nam Từ Liêm, Hà Nội")
                    .startTime(LocalDateTime.now().plusDays(30).withHour(19).withMinute(0))
                    .endTime(LocalDateTime.now().plusDays(30).withHour(23).withMinute(0))
                    .status(EventStatus.ACTIVE) // Đang mở cổng bán vé
                    .build();

            Event savedEvent = eventRepository.save(concert);

            // 4. Khởi tạo các Hạng vé cho Concert
            TicketType vvip = TicketType.builder()
                    .event(savedEvent)
                    .name("VVIP Lounge (Ghế sofa, Set ăn VIP & Giao lưu thần tượng)")
                    .description("Vị trí sát sân khấu chính, phục vụ đồ uống và tiệc nhẹ riêng biệt")
                    .price(new BigDecimal("10000000"))
                    .totalQuantity(10) // Số lượng giới hạn cực hiếm để test Flash Sale cháy vé
                    .availableQuantity(10)
                    .build();

            TicketType vip1 = TicketType.builder()
                    .event(savedEvent)
                    .name("VIP 1 (Khán đài trung tâm A, Soundcheck)")
                    .description("Chỗ ngồi trực diện sân khấu, tặng kèm lighstick chính hãng và quyền xem soundcheck")
                    .price(new BigDecimal("4500000"))
                    .totalQuantity(50)
                    .availableQuantity(50)
                    .build();

            TicketType ga = TicketType.builder()
                    .event(savedEvent)
                    .name("GA Standing (Khu vực đứng sát đường chạy)")
                    .description("Khu vực đứng không chia số ghế, quẩy tưng bừng theo từng điệu nhạc")
                    .price(new BigDecimal("1800000"))
                    .totalQuantity(200)
                    .availableQuantity(200)
                    .build();

            ticketTypeRepository.saveAll(Set.of(vvip, vip1, ga));
            log.info("Đã tạo sự kiện '{}' cùng 3 hạng vé: VVIP (10 vé), VIP 1 (50 vé), GA (200 vé)", savedEvent.getName());
        }

        log.info("===> Hoàn tất nạp dữ liệu mẫu cho Dự án 5!");
    }
}
