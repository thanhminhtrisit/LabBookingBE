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

    @Column(name = "student_code", length = 50)
    private String studentCode;  // e.g. "CS/2023/0142" — used when role = MEMBER

    @Column(name = "staff_code", length = 50)
    private String staffCode;    // e.g. "STF/2015/0023" — used when role = STAFF or ADMIN

    @Column(length = 100)
    private String department;   // e.g. "Software Engineering", "Computer Networks"

    @Column(length = 100)
    private String faculty;      // e.g. "Computer Science & IT", "Applied Sciences"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
