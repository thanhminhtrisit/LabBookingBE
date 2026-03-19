CREATE TABLE users (
                       id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                       full_name  VARCHAR(100)                        NOT NULL,
                       email      VARCHAR(150)                        NOT NULL UNIQUE,
                       password   VARCHAR(255)                        NOT NULL,
                       phone      VARCHAR(20),
                       role       ENUM('MEMBER','STAFF','ADMIN')       NOT NULL DEFAULT 'MEMBER',
                       created_at DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE labs (
                      id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name        VARCHAR(100)                              NOT NULL,
                      code        VARCHAR(20)                               NOT NULL UNIQUE,
                      location    VARCHAR(200),
                      description TEXT,
                      capacity    INT,
                      status      ENUM('ACTIVE','MAINTENANCE','CLOSED')     NOT NULL DEFAULT 'ACTIVE',
                      created_at  DATETIME                                  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
                          id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id     BIGINT                                          NOT NULL,
                          lab_id      BIGINT                                          NOT NULL,
                          start_time  DATETIME                                        NOT NULL,
                          end_time    DATETIME                                        NOT NULL,
                          title       VARCHAR(200)                                    NOT NULL,
                          note        TEXT,
                          status      ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
                          reviewed_by BIGINT,
                          reviewed_at DATETIME,
                          created_at  DATETIME                                        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_booking_user FOREIGN KEY (user_id)     REFERENCES users(id),
                          CONSTRAINT fk_booking_lab  FOREIGN KEY (lab_id)      REFERENCES labs(id),
                          CONSTRAINT fk_booking_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
                          INDEX idx_booking_lab_time (lab_id, start_time, end_time)
);

CREATE TABLE notifications (
                               id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id    BIGINT                                                                          NOT NULL,
                               booking_id BIGINT,
                               type       ENUM('BOOKING_APPROVED','BOOKING_REJECTED','BOOKING_CANCELLED','LAB_MAINTENANCE','LAB_CLOSED','BOOKING_IN_PROGRESS') NOT NULL,
                               title      VARCHAR(200)                                                                    NOT NULL,
                               message    TEXT                                                                            NOT NULL,
                               is_read    BOOLEAN                                                                         NOT NULL DEFAULT FALSE,
                               created_at DATETIME                                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notif_user    FOREIGN KEY (user_id)    REFERENCES users(id),
                               CONSTRAINT fk_notif_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
                               INDEX idx_notif_user (user_id, is_read)
);

-- Seed admin account mặc định (password: Admin@123 — BCrypt)
INSERT INTO users (full_name, email, password, role)
VALUES ('Administrator', 'admin@lab.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMIN');

-- Seed labs mẫu
INSERT INTO labs (name, code, location, description, capacity, status)
VALUES
    ('Lab Công nghệ Thông tin 1', 'LAB001', 'Tầng 3, Phòng 301', 'Lab máy tính với 30 máy, hỗ trợ lập trình và thiết kế', 30, 'ACTIVE'),
    ('Lab Công nghệ Thông tin 2', 'LAB002', 'Tầng 3, Phòng 302', 'Lab máy tính với 25 máy, chuyên về mạng và bảo mật', 25, 'ACTIVE'),
    ('Lab Công nghệ Thông tin 3', 'LAB003', 'Tầng 4, Phòng 401', 'Lab máy tính với 20 máy, dành cho nghiên cứu AI', 20, 'MAINTENANCE'),
    ('Lab Công nghệ Thông tin 4', 'LAB004', 'Tầng 4, Phòng 402', 'Lab máy tính với 15 máy, phòng thí nghiệm hóa học ảo', 15, 'CLOSED');
