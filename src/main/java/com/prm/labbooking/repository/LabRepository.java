package com.prm.labbooking.repository;
import com.prm.labbooking.emus.LabStatus;
import com.prm.labbooking.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByStatus(LabStatus status);
    Optional<Lab> findByCode(String code);
}
