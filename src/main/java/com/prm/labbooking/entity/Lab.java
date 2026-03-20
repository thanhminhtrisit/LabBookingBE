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

    @Column(length = 100)
    private String building;   // e.g. "Block C", "Block E"

    @Column(length = 100)
    private String faculty;    // e.g. "Computer Science & IT", "Applied Sciences"

    @Column(columnDefinition = "TEXT")
    private String equipment;  // JSON string, e.g. [{"name":"Desktop PC","quantity":30}]

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabStatus status = LabStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
