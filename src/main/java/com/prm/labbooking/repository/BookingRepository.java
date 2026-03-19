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
