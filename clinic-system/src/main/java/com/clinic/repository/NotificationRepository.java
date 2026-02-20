package com.clinic.repository;

import com.clinic.model.entity.Notification;
import com.clinic.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderBySentAtDesc(User user);

    List<Notification> findBySentAtBetween(LocalDateTime from, LocalDateTime to);

    /** Delivery rate = success count / total count in a period — used by admin dashboard */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.sentAt >= :from AND n.success = true")
    long countSuccessfulSince(LocalDateTime from);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.sentAt >= :from")
    long countTotalSince(LocalDateTime from);
}
