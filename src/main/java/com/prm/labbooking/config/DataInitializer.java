package com.prm.labbooking.config;

import com.prm.labbooking.entity.*;
import com.prm.labbooking.emus.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.prm.labbooking.repository.*;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Seed admin account
            if (userRepository.findByEmail("admin@lab.com").isEmpty()) {
                User admin = User.builder()
                    .fullName("Administrator")
                    .email("admin@lab.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();
                userRepository.save(admin);
                System.out.println("Seeded admin account: admin@lab.com / Admin@123");
            }

            // Seed labs
            if (labRepository.count() == 0) {
                Lab lab1 = Lab.builder()
                    .name("Lab Công nghệ Thông tin 1")
                    .code("LAB001")
                    .location("Tầng 3, Phòng 301")
                    .description("Lab máy tính với 30 máy, hỗ trợ lập trình và thiết kế")
                    .capacity(30)
                    .status(LabStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

                Lab lab2 = Lab.builder()
                    .name("Lab Công nghệ Thông tin 2")
                    .code("LAB002")
                    .location("Tầng 3, Phòng 302")
                    .description("Lab máy tính với 25 máy, chuyên về mạng và bảo mật")
                    .capacity(25)
                    .status(LabStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

                Lab lab3 = Lab.builder()
                    .name("Lab Công nghệ Thông tin 3")
                    .code("LAB003")
                    .location("Tầng 4, Phòng 401")
                    .description("Lab máy tính với 20 máy, dành cho nghiên cứu AI")
                    .capacity(20)
                    .status(LabStatus.MAINTENANCE)
                    .createdAt(LocalDateTime.now())
                    .build();

                Lab lab4 = Lab.builder()
                    .name("Lab Công nghệ Thông tin 4")
                    .code("LAB004")
                    .location("Tầng 4, Phòng 402")
                    .description("Lab máy tính với 15 máy, phòng thí nghiệm hóa học ảo")
                    .capacity(15)
                    .status(LabStatus.CLOSED)
                    .createdAt(LocalDateTime.now())
                    .build();

                labRepository.save(lab1);
                labRepository.save(lab2);
                labRepository.save(lab3);
                labRepository.save(lab4);
                System.out.println("Seeded 4 sample labs");
            }
        };
    }
}
