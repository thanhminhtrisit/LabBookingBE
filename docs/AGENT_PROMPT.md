# 🤖 AGENT PROMPT — Lab Booking API (Spring Boot 4.0.3 + Java 21)

> Đọc toàn bộ project hiện tại trước khi làm bất cứ điều gì.
> Tuân thủ đúng kiến trúc, package name, và naming convention đã có trong project.
> KHÔNG tạo thêm bảng DB, KHÔNG đổi tên package, KHÔNG thay đổi BaseResponse.

---

## 📁 Kiến trúc project (đã có, giữ nguyên)

```
com.prm.labbooking
├── config/           ← SecurityConfig, JwtConfig, OpenApiConfig
├── controller/       ← REST controllers
├── dto/              ← Internal DTOs nếu cần
├── emus/             ← Enums: Role, LabStatus, BookingStatus, NotificationType
├── entity/           ← JPA entities
├── exception/        ← Custom exceptions + GlobalExceptionHandler
├── mapper/           ← Manual mappers (Entity <-> Response)
├── payloads/
│   ├── request/      ← Request body classes
│   └── response/
│       └── BaseResponse.java  ← ĐÃ CÓ, KHÔNG SỬA
├── repository/       ← Spring Data JPA repositories
├── service/
│   ├── impl/         ← UserServiceImpl.java đã có
│   └── UserService.java       ← Interface đã có
├── utils/            ← JwtUtils, ResponseUtils
└── LabbookingApplication.java
```

**BaseResponse đã có — KHÔNG SỬA:**
```java
package com.prm.labbooking.payloads.response;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BaseResponse {
    private String message;
    private String statusCode;
    private Object data;
}
```

---

## 🗄️ Database schema (4 bảng — KHÔNG thêm bảng khác)

File `src/main/resources/db/migration/V1__init_schema.sql` đã có:

```
users:         id, full_name, email, password, phone, role(ENUM), created_at
labs:          id, name, code, location, description, capacity, status(ENUM), created_at
bookings:      id, user_id(FK), lab_id(FK), start_time, end_time, title, note,
               status(ENUM), reviewed_by(FK nullable), reviewed_at, created_at
notifications: id, user_id(FK), booking_id(FK nullable), type(ENUM),
               title, message, is_read, created_at
```

---

## 📦 Tech stack (từ pom.xml đã có — KHÔNG thêm dependency mới)

- Spring Boot **4.0.3**, Java **21**
- spring-boot-starter-webmvc
- spring-boot-starter-data-jpa
- spring-boot-starter-security + spring-boot-starter-validation
- spring-boot-starter-flyway + flyway-mysql
- springdoc-openapi-starter-webmvc-ui **3.0.2**
- JJWT **0.11.5** — dùng `Jwts.parserBuilder()` KHÔNG phải `Jwts.parser()`
- Lombok, MySQL connector

---

## 🔧 BƯỚC 1 — Tạo file .env ở root project (cùng cấp pom.xml)

```env
# .env
DB_URL=jdbc:mysql://localhost:3306/lab_booking_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=lab-booking-super-secret-key-minimum-256-bits-long-string-here
JWT_EXPIRATION=86400000
SERVER_PORT=8080
APP_NAME=lab-booking-api
```

Tạo thêm `.env.example` (commit lên git, không có giá trị thật):
```env
# .env.example — copy sang .env và điền giá trị thật
DB_URL=jdbc:mysql://localhost:3306/lab_booking_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION=86400000
SERVER_PORT=8080
APP_NAME=lab-booking-api
```

Thêm vào `.gitignore` (tạo nếu chưa có):
```
.env
target/
*.class
```

---

## 🔧 BƯỚC 2 — Cập nhật application.yml đọc từ .env

```yaml
spring:
  application:
    name: ${APP_NAME:lab-booking-api}

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        default_batch_fetch_size: 20

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  jackson:
    time-zone: Asia/Ho_Chi_Minh
    date-format: yyyy-MM-dd HH:mm:ss
    serialization:
      write-dates-as-timestamps: false

server:
  port: ${SERVER_PORT:8080}

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs

logging:
  level:
    com.prm.labbooking: DEBUG
    org.springframework.security: INFO
```

---

## 🔧 BƯỚC 3 — Cập nhật LabbookingApplication.java (load .env thủ công)

```java
package com.prm.labbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.*;

@SpringBootApplication
public class LabbookingApplication {

    public static void main(String[] args) {
        loadEnv();
        SpringApplication.run(LabbookingApplication.class, args);
    }

    private static void loadEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx < 0) continue;
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (System.getenv(key) == null) System.setProperty(key, value);
            }
        } catch (Exception e) {
            System.err.println("[.env] Could not load: " + e.getMessage());
        }
    }
}
```

---

## 🔧 BƯỚC 4 — Tạo Enums trong package `emus`

```java
// emus/Role.java
package com.prm.labbooking.emus;
public enum Role { MEMBER, STAFF, ADMIN }

// emus/LabStatus.java
package com.prm.labbooking.emus;
public enum LabStatus { ACTIVE, MAINTENANCE, CLOSED }

// emus/BookingStatus.java
package com.prm.labbooking.emus;
public enum BookingStatus { PENDING, APPROVED, REJECTED, CANCELLED }

// emus/NotificationType.java
package com.prm.labbooking.emus;
public enum NotificationType {
    BOOKING_APPROVED, BOOKING_REJECTED, BOOKING_CANCELLED,
    LAB_MAINTENANCE, LAB_CLOSED
}
```

---

## 🔧 BƯỚC 5 — Tạo Entities trong package `entity`

### User.java
```java
package com.prm.labbooking.entity;

import com.prm.labbooking.emus.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### Lab.java
```java
package com.prm.labbooking.entity;

import com.prm.labbooking.emus.LabStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "labs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lab {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabStatus status = LabStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### Booking.java
```java
package com.prm.labbooking.entity;

import com.prm.labbooking.emus.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### Notification.java
```java
package com.prm.labbooking.entity;

import com.prm.labbooking.emus.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

## 🔧 BƯỚC 6 — Payloads

### payloads/request/RegisterRequest.java
```java
@Getter @Setter
public class RegisterRequest {
    @NotBlank(message = "Full name is required") private String fullName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") private String password;
    private String phone;
}
```

### payloads/request/LoginRequest.java
```java
@Getter @Setter
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
}
```

### payloads/request/CreateLabRequest.java
```java
@Getter @Setter
public class CreateLabRequest {
    @NotBlank private String name;
    @NotBlank private String code;
    private String location;
    private String description;
    private Integer capacity;
}
```

### payloads/request/UpdateLabRequest.java
```java
@Getter @Setter
public class UpdateLabRequest {
    private String name;
    private String location;
    private String description;
    private Integer capacity;
}
```

### payloads/request/UpdateLabStatusRequest.java
```java
@Getter @Setter
public class UpdateLabStatusRequest {
    @NotNull(message = "Status is required") private LabStatus status;
    private String message;
}
```

### payloads/request/CreateBookingRequest.java
```java
@Getter @Setter
public class CreateBookingRequest {
    @NotNull private Long labId;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @NotBlank private String title;
    private String note;
}
```

### payloads/request/RejectBookingRequest.java
```java
@Getter @Setter
public class RejectBookingRequest {
    private String reason;
}
```

### payloads/request/UpdateRoleRequest.java
```java
@Getter @Setter
public class UpdateRoleRequest {
    @NotNull(message = "Role is required") private Role role;
}
```

### payloads/response/ — data classes (KHÔNG sửa BaseResponse)
```java
// AuthResponse.java
@Getter @Setter @Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
}

// UserResponse.java
@Getter @Setter @Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}

// LabResponse.java
@Getter @Setter @Builder
public class LabResponse {
    private Long id;
    private String name;
    private String code;
    private String location;
    private String description;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
}

// BookingResponse.java
@Getter @Setter @Builder
public class BookingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long labId;
    private String labName;
    private String labLocation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title;
    private String note;
    private String status;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}

// NotificationResponse.java
@Getter @Setter @Builder
public class NotificationResponse {
    private Long id;
    private Long bookingId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
```

---

## 🔧 BƯỚC 7 — Utils

### utils/ResponseUtils.java
```java
package com.prm.labbooking.utils;

import com.prm.labbooking.payloads.response.BaseResponse;

public class ResponseUtils {

    public static BaseResponse success(Object data, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("200"); r.setMessage(message); r.setData(data);
        return r;
    }

    public static BaseResponse created(Object data, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("201"); r.setMessage(message); r.setData(data);
        return r;
    }

    public static BaseResponse error(String statusCode, String message) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode(statusCode); r.setMessage(message); r.setData(null);
        return r;
    }
}
```

### utils/JwtUtils.java
**QUAN TRỌNG: JJWT 0.11.5 — dùng `Jwts.parserBuilder()` không phải `Jwts.parser()`**

```java
package com.prm.labbooking.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        String encoded = Base64.getEncoder().encodeToString(secret.getBytes());
        byte[] keyBytes = Decoders.BASE64.decode(encoded);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("email", email)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try { extractAllClaims(token); return true; }
        catch (Exception e) { return false; }
    }
}
```

---

## 🔧 BƯỚC 8 — Security

### security/UserPrincipal.java
```java
package com.prm.labbooking.security;

import lombok.Getter;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

### security/JwtAuthenticationFilter.java
```java
package com.prm.labbooking.security;

import com.prm.labbooking.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (jwtUtils.isTokenValid(token)) {
            Long userId = jwtUtils.extractUserId(token);
            String role  = jwtUtils.extractRole(token);
            Claims claims = jwtUtils.extractAllClaims(token);
            String email = claims.get("email", String.class);

            UserPrincipal principal = new UserPrincipal(userId, email, role);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
```

### config/SecurityConfig.java
```java
package com.prm.labbooking.config;

import com.prm.labbooking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/labs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/labs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/labs/*/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/approve").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/reject").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/bookings/pending").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/bookings").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### config/OpenApiConfig.java
```java
package com.prm.labbooking.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Lab Booking API").version("1.0").description("Lab Booking System"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

---

## 🔧 BƯỚC 9 — Repositories

```java
// UserRepository.java
package com.prm.labbooking.repository;
import com.prm.labbooking.emus.Role;
import com.prm.labbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
}

// LabRepository.java
package com.prm.labbooking.repository;
import com.prm.labbooking.emus.LabStatus;
import com.prm.labbooking.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByStatus(LabStatus status);
    Optional<Lab> findByCode(String code);
}

// BookingRepository.java
package com.prm.labbooking.repository;
import com.prm.labbooking.emus.BookingStatus;
import com.prm.labbooking.entity.Booking;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.lab.id = :labId " +
           "AND b.status = 'APPROVED' " +
           "AND b.startTime < :endTime " +
           "AND b.endTime > :startTime")
    boolean existsConflict(@Param("labId") Long labId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.lab.id = :labId " +
           "AND b.status = 'APPROVED' " +
           "AND FUNCTION('DATE', b.startTime) = :date")
    List<Booking> findApprovedByLabAndDate(@Param("labId") Long labId,
                                           @Param("date") LocalDate date);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.lab.id = :labId " +
           "AND b.status IN ('PENDING', 'APPROVED') " +
           "AND b.startTime > :now")
    List<Booking> findFutureActiveByLabId(@Param("labId") Long labId,
                                          @Param("now") LocalDateTime now);
}

// NotificationRepository.java
package com.prm.labbooking.repository;
import com.prm.labbooking.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}
```

---

## 🔧 BƯỚC 10 — Services (interface + impl)

Mỗi service có file interface trong `service/` và implementation trong `service/impl/`.
UserService.java và UserServiceImpl.java đã có — KHÔNG tạo lại, chỉ bổ sung nếu thiếu method.

### service/AuthService.java + service/impl/AuthServiceImpl.java
```java
// Interface
public interface AuthService {
    BaseResponse register(RegisterRequest request);
    BaseResponse login(LoginRequest request);
    BaseResponse getMe(Long userId);
}

// Implementation
@Service @RequiredArgsConstructor @Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public BaseResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            return ResponseUtils.error("409", "Email đã được sử dụng");

        User user = User.builder()
            .fullName(request.getFullName()).email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone()).role(Role.MEMBER).build();
        User saved = userRepository.save(user);
        String token = jwtUtils.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return ResponseUtils.created(
            AuthResponse.builder().token(token).tokenType("Bearer").user(toUserResponse(saved)).build(),
            "Đăng ký thành công");
    }

    @Override
    public BaseResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword()))
            return ResponseUtils.error("401", "Email hoặc mật khẩu không đúng");

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseUtils.success(
            AuthResponse.builder().token(token).tokenType("Bearer").user(toUserResponse(user)).build(),
            "Đăng nhập thành công");
    }

    @Override
    public BaseResponse getMe(Long userId) {
        return userRepository.findById(userId)
            .map(u -> ResponseUtils.success(toUserResponse(u), "OK"))
            .orElse(ResponseUtils.error("404", "User không tồn tại"));
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
            .id(u.getId()).fullName(u.getFullName()).email(u.getEmail())
            .phone(u.getPhone()).role(u.getRole().name()).createdAt(u.getCreatedAt()).build();
    }
}
```

### service/NotificationService.java + service/impl/NotificationServiceImpl.java
```java
// Interface
public interface NotificationService {
    void sendToUser(User user, Booking booking, NotificationType type, String title, String message);
    BaseResponse getMyNotifications(Long userId);
    BaseResponse markAsRead(Long notificationId, Long userId);
    BaseResponse markAllAsRead(Long userId);
}

// Implementation
@Service @RequiredArgsConstructor @Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    public void sendToUser(User user, Booking booking, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
            .user(user).booking(booking).type(type)
            .title(title).message(message).isRead(false).build());
    }

    @Override
    public BaseResponse getMyNotifications(Long userId) {
        List<NotificationResponse> data = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).toList();
        return ResponseUtils.success(data, "OK");
    }

    @Override
    public BaseResponse markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId).orElse(null);
        if (n == null) return ResponseUtils.error("404", "Notification không tồn tại");
        if (!n.getUser().getId().equals(userId)) return ResponseUtils.error("403", "Không có quyền");
        n.setIsRead(true);
        return ResponseUtils.success(toResponse(notificationRepository.save(n)), "OK");
    }

    @Override
    public BaseResponse markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream().filter(n -> !n.getIsRead()).toList();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        return ResponseUtils.success(Map.of("updatedCount", unread.size()), "Đã đánh dấu tất cả đã đọc");
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .bookingId(n.getBooking() != null ? n.getBooking().getId() : null)
            .type(n.getType().name()).title(n.getTitle()).message(n.getMessage())
            .isRead(n.getIsRead()).createdAt(n.getCreatedAt()).build();
    }
}
```

### service/LabService.java + service/impl/LabServiceImpl.java
```java
// Interface
public interface LabService {
    BaseResponse getAllLabs(String status);
    BaseResponse getLabById(Long id);
    BaseResponse getLabSchedule(Long labId, LocalDate date);
    BaseResponse createLab(CreateLabRequest request);
    BaseResponse updateLab(Long id, UpdateLabRequest request);
    BaseResponse updateLabStatus(Long id, UpdateLabStatusRequest request);
}

// Implementation
@Service @RequiredArgsConstructor @Transactional
public class LabServiceImpl implements LabService {
    private final LabRepository labRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Override
    public BaseResponse getAllLabs(String statusStr) {
        List<Lab> labs;
        if (statusStr != null) {
            try { labs = labRepository.findByStatus(LabStatus.valueOf(statusStr.toUpperCase())); }
            catch (IllegalArgumentException e) { return ResponseUtils.error("400", "Status không hợp lệ"); }
        } else {
            labs = labRepository.findByStatus(LabStatus.ACTIVE);
        }
        return ResponseUtils.success(labs.stream().map(this::toLabResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getLabById(Long id) {
        return labRepository.findById(id)
            .map(l -> ResponseUtils.success(toLabResponse(l), "OK"))
            .orElse(ResponseUtils.error("404", "Lab không tồn tại"));
    }

    @Override
    public BaseResponse getLabSchedule(Long labId, LocalDate date) {
        if (!labRepository.existsById(labId)) return ResponseUtils.error("404", "Lab không tồn tại");
        List<Booking> bookings = bookingRepository.findApprovedByLabAndDate(labId, date);
        return ResponseUtils.success(bookings.stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse createLab(CreateLabRequest request) {
        if (labRepository.findByCode(request.getCode()).isPresent())
            return ResponseUtils.error("409", "Mã lab đã tồn tại");
        Lab lab = Lab.builder().name(request.getName()).code(request.getCode())
            .location(request.getLocation()).description(request.getDescription())
            .capacity(request.getCapacity()).status(LabStatus.ACTIVE).build();
        return ResponseUtils.created(toLabResponse(labRepository.save(lab)), "Tạo lab thành công");
    }

    @Override
    public BaseResponse updateLab(Long id, UpdateLabRequest request) {
        Lab lab = labRepository.findById(id).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        if (request.getName() != null) lab.setName(request.getName());
        if (request.getLocation() != null) lab.setLocation(request.getLocation());
        if (request.getDescription() != null) lab.setDescription(request.getDescription());
        if (request.getCapacity() != null) lab.setCapacity(request.getCapacity());
        return ResponseUtils.success(toLabResponse(labRepository.save(lab)), "Cập nhật thành công");
    }

    @Override
    @Transactional
    public BaseResponse updateLabStatus(Long id, UpdateLabStatusRequest request) {
        Lab lab = labRepository.findById(id).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        lab.setStatus(request.getStatus());
        labRepository.save(lab);

        int affected = 0;
        if (request.getStatus() == LabStatus.MAINTENANCE || request.getStatus() == LabStatus.CLOSED) {
            List<Booking> futures = bookingRepository.findFutureActiveByLabId(id, LocalDateTime.now());
            NotificationType nType = request.getStatus() == LabStatus.MAINTENANCE
                ? NotificationType.LAB_MAINTENANCE : NotificationType.LAB_CLOSED;
            String nTitle = "Lab " + lab.getName() + " tạm ngừng hoạt động";
            String nMsg = request.getMessage() != null ? request.getMessage()
                : "Booking của bạn đã bị hủy do lab tạm ngừng.";
            for (Booking b : futures) {
                b.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(b);
                notificationService.sendToUser(b.getUser(), b, nType, nTitle, nMsg);
                affected++;
            }
        }
        return ResponseUtils.success(Map.of("lab", toLabResponse(lab), "affectedBookings", affected),
            "Cập nhật trạng thái thành công");
    }

    private LabResponse toLabResponse(Lab l) {
        return LabResponse.builder().id(l.getId()).name(l.getName()).code(l.getCode())
            .location(l.getLocation()).description(l.getDescription()).capacity(l.getCapacity())
            .status(l.getStatus().name()).createdAt(l.getCreatedAt()).build();
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder().id(b.getId())
            .userId(b.getUser().getId()).userName(b.getUser().getFullName())
            .labId(b.getLab().getId()).labName(b.getLab().getName()).labLocation(b.getLab().getLocation())
            .startTime(b.getStartTime()).endTime(b.getEndTime())
            .title(b.getTitle()).note(b.getNote())
            .status(b.getStatus().name()).createdAt(b.getCreatedAt()).build();
    }
}
```

### service/BookingService.java + service/impl/BookingServiceImpl.java
```java
// Interface
public interface BookingService {
    BaseResponse createBooking(CreateBookingRequest request, Long userId);
    BaseResponse getMyBookings(Long userId);
    BaseResponse getPendingBookings();
    BaseResponse getAllBookings();
    BaseResponse approveBooking(Long bookingId, Long reviewerId);
    BaseResponse rejectBooking(Long bookingId, Long reviewerId, RejectBookingRequest request);
    BaseResponse cancelBooking(Long bookingId, Long userId);
}

// Implementation
@Service @RequiredArgsConstructor @Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public BaseResponse createBooking(CreateBookingRequest req, Long userId) {
        if (!req.getStartTime().isBefore(req.getEndTime()))
            return ResponseUtils.error("400", "Thời gian bắt đầu phải trước thời gian kết thúc");

        Lab lab = labRepository.findById(req.getLabId()).orElse(null);
        if (lab == null) return ResponseUtils.error("404", "Lab không tồn tại");
        if (lab.getStatus() != LabStatus.ACTIVE)
            return ResponseUtils.error("400", "Lab hiện không hoạt động");
        if (bookingRepository.existsConflict(req.getLabId(), req.getStartTime(), req.getEndTime()))
            return ResponseUtils.error("409", "Lab đã có booking trong khung giờ này");

        User user = userRepository.findById(userId).orElseThrow();
        Booking saved = bookingRepository.save(Booking.builder()
            .user(user).lab(lab).startTime(req.getStartTime()).endTime(req.getEndTime())
            .title(req.getTitle()).note(req.getNote()).status(BookingStatus.PENDING).build());

        // Notify ADMIN + STAFF
        List<User> receivers = userRepository.findByRole(Role.ADMIN);
        receivers.addAll(userRepository.findByRole(Role.STAFF));
        for (User r : receivers) {
            notificationService.sendToUser(r, saved, NotificationType.BOOKING_APPROVED,
                "Yêu cầu booking mới",
                user.getFullName() + " đã gửi yêu cầu đặt lab " + lab.getName());
        }
        return ResponseUtils.created(toBookingResponse(saved), "Gửi yêu cầu booking thành công");
    }

    @Override
    public BaseResponse getMyBookings(Long userId) {
        return ResponseUtils.success(
            bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getPendingBookings() {
        return ResponseUtils.success(
            bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING)
                .stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse getAllBookings() {
        return ResponseUtils.success(
            bookingRepository.findAll().stream().map(this::toBookingResponse).toList(), "OK");
    }

    @Override
    public BaseResponse approveBooking(Long bookingId, Long reviewerId) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (b.getStatus() != BookingStatus.PENDING)
            return ResponseUtils.error("400", "Chỉ có thể duyệt booking đang PENDING");
        if (bookingRepository.existsConflict(b.getLab().getId(), b.getStartTime(), b.getEndTime()))
            return ResponseUtils.error("409", "Khung giờ này đã bị booking khác chiếm trước");

        User reviewer = userRepository.findById(reviewerId).orElseThrow();
        b.setStatus(BookingStatus.APPROVED);
        b.setReviewedBy(reviewer);
        b.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(b);

        notificationService.sendToUser(b.getUser(), b, NotificationType.BOOKING_APPROVED,
            "Booking được duyệt",
            "Booking lab " + b.getLab().getName() + " của bạn đã được duyệt.");
        return ResponseUtils.success(toBookingResponse(b), "Duyệt booking thành công");
    }

    @Override
    public BaseResponse rejectBooking(Long bookingId, Long reviewerId, RejectBookingRequest request) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (b.getStatus() != BookingStatus.PENDING)
            return ResponseUtils.error("400", "Chỉ có thể từ chối booking đang PENDING");

        User reviewer = userRepository.findById(reviewerId).orElseThrow();
        b.setStatus(BookingStatus.REJECTED);
        b.setReviewedBy(reviewer);
        b.setReviewedAt(LocalDateTime.now());
        bookingRepository.save(b);

        String reason = (request != null && request.getReason() != null)
            ? request.getReason() : "Không có lý do cụ thể";
        notificationService.sendToUser(b.getUser(), b, NotificationType.BOOKING_REJECTED,
            "Booking bị từ chối",
            "Booking lab " + b.getLab().getName() + " bị từ chối. Lý do: " + reason);
        return ResponseUtils.success(toBookingResponse(b), "Từ chối booking thành công");
    }

    @Override
    public BaseResponse cancelBooking(Long bookingId, Long userId) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) return ResponseUtils.error("404", "Booking không tồn tại");
        if (!b.getUser().getId().equals(userId))
            return ResponseUtils.error("403", "Bạn không có quyền hủy booking này");
        if (b.getStatus() == BookingStatus.REJECTED || b.getStatus() == BookingStatus.CANCELLED)
            return ResponseUtils.error("400", "Booking không thể hủy ở trạng thái hiện tại");

        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);

        List<User> receivers = userRepository.findByRole(Role.ADMIN);
        receivers.addAll(userRepository.findByRole(Role.STAFF));
        for (User r : receivers) {
            notificationService.sendToUser(r, b, NotificationType.BOOKING_CANCELLED,
                "Booking bị hủy",
                b.getUser().getFullName() + " đã hủy booking lab " + b.getLab().getName());
        }
        return ResponseUtils.success(toBookingResponse(b), "Hủy booking thành công");
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder().id(b.getId())
            .userId(b.getUser().getId()).userName(b.getUser().getFullName())
            .labId(b.getLab().getId()).labName(b.getLab().getName()).labLocation(b.getLab().getLocation())
            .startTime(b.getStartTime()).endTime(b.getEndTime())
            .title(b.getTitle()).note(b.getNote()).status(b.getStatus().name())
            .reviewedByName(b.getReviewedBy() != null ? b.getReviewedBy().getFullName() : null)
            .reviewedAt(b.getReviewedAt()).createdAt(b.getCreatedAt()).build();
    }
}
```

---

## 🔧 BƯỚC 11 — Controllers

Mọi Controller đều dùng helper này để lấy userId từ token:
```java
private Long getCurrentUserId() {
    UserPrincipal p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return p.getId();
}
```

### AuthController.java
```java
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng ký, đăng nhập")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse> me() {
        return ResponseEntity.ok(authService.getMe(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserPrincipal p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return p.getId();
    }
}
```

### LabController.java
```java
@RestController @RequestMapping("/api/labs") @RequiredArgsConstructor
@Tag(name = "Labs", description = "Quản lý lab")
public class LabController {
    private final LabService labService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(labService.getAllLabs(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getLabById(id));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<BaseResponse> schedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(labService.getLabSchedule(id, date));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody CreateLabRequest req) {
        return ResponseEntity.ok(labService.createLab(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(@PathVariable Long id,
                                                @RequestBody UpdateLabRequest req) {
        return ResponseEntity.ok(labService.updateLab(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateLabStatusRequest req) {
        return ResponseEntity.ok(labService.updateLabStatus(id, req));
    }
}
```

### BookingController.java
```java
@RestController @RequestMapping("/api/bookings") @RequiredArgsConstructor
@Tag(name = "Bookings", description = "Đặt phòng lab")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(req, getCurrentUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<BaseResponse> myBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings(getCurrentUserId()));
    }

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse> pending() {
        return ResponseEntity.ok(bookingService.getPendingBookings());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> all() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<BaseResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.approveBooking(id, getCurrentUserId()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BaseResponse> reject(@PathVariable Long id,
                                                @RequestBody(required = false) RejectBookingRequest req) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, getCurrentUserId(), req));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BaseResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserPrincipal p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return p.getId();
    }
}
```

### NotificationController.java
```java
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAll() {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<BaseResponse> read(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, getCurrentUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse> readAll() {
        return ResponseEntity.ok(notificationService.markAllAsRead(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        UserPrincipal p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return p.getId();
    }
}
```

### AdminController.java
```java
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
@Tag(name = "Admin", description = "Quản trị hệ thống")
public class AdminController {
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<BaseResponse> getUsers() {
        List<UserResponse> data = userRepository.findAll().stream()
            .map(u -> UserResponse.builder().id(u.getId()).fullName(u.getFullName())
                .email(u.getEmail()).phone(u.getPhone()).role(u.getRole().name())
                .createdAt(u.getCreatedAt()).build()).toList();
        BaseResponse r = new BaseResponse();
        r.setStatusCode("200"); r.setMessage("OK"); r.setData(data);
        return ResponseEntity.ok(r);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<BaseResponse> updateRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateRoleRequest request) {
        BaseResponse r = new BaseResponse();
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            r.setStatusCode("404"); r.setMessage("User không tồn tại");
            return ResponseEntity.ok(r);
        }
        user.setRole(request.getRole());
        userRepository.save(user);
        r.setStatusCode("200"); r.setMessage("Cập nhật role thành công");
        r.setData(Map.of("userId", id, "newRole", request.getRole().name()));
        return ResponseEntity.ok(r);
    }
}
```

---

## 🔧 BƯỚC 12 — Global Exception Handler

```java
package com.prm.labbooking.exception;

import com.prm.labbooking.payloads.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        BaseResponse r = new BaseResponse();
        r.setStatusCode("400"); r.setMessage(message);
        return ResponseEntity.badRequest().body(r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse> handleAccessDenied(AccessDeniedException ex) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("403"); r.setMessage("Bạn không có quyền thực hiện hành động này");
        return ResponseEntity.status(403).body(r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneral(Exception ex) {
        BaseResponse r = new BaseResponse();
        r.setStatusCode("500"); r.setMessage("Lỗi server: " + ex.getMessage());
        return ResponseEntity.status(500).body(r);
    }
}
```

---

## ✅ Checklist — Kiểm tra trước khi hoàn thành

- [ ] `.env` đã có ở root project, `.env.example` đã có, `.env` đã thêm vào `.gitignore`
- [ ] `LabbookingApplication.java` đã có method `loadEnv()`
- [ ] `application.yml` đọc từ env variables `${DB_URL}`, `${JWT_SECRET}` v.v.
- [ ] 4 enums trong package `emus` (Role, LabStatus, BookingStatus, NotificationType)
- [ ] 4 entity classes trong `entity` (User, Lab, Booking, Notification)
- [ ] Tất cả request classes trong `payloads/request/`
- [ ] Tất cả response classes trong `payloads/response/` — KHÔNG sửa BaseResponse
- [ ] `ResponseUtils` trong `utils/`
- [ ] `JwtUtils` trong `utils/` — dùng `Jwts.parserBuilder()` (JJWT 0.11.5)
- [ ] `UserPrincipal` và `JwtAuthenticationFilter` trong `security/`
- [ ] `SecurityConfig` và `OpenApiConfig` trong `config/`
- [ ] 4 Repository interfaces trong `repository/`
- [ ] Service interfaces trong `service/`, implementations trong `service/impl/`
- [ ] 5 Controller classes trong `controller/`
- [ ] `GlobalExceptionHandler` trong `exception/`
- [ ] `UserService` và `UserServiceImpl` không bị tạo lại — kiểm tra trùng lặp
- [ ] `ddl-auto: validate` trong `application.yml`
- [ ] Build thành công: `mvn clean compile` không có lỗi
