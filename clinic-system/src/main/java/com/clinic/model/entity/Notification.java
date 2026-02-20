package com.clinic.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification — audit log of every email/SMS sent by the system.
 *
 * ISO/IEC 27001 requirement: all automated communications must be logged
 * so the clinic can verify reminder delivery and investigate failures.
 *
 * Admin dashboard queries this table for the "Notification Delivery Rate" metric.
 */
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notif_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** EMAIL | SMS */
    @Column(nullable = false, length = 10)
    private String channel;

    /** REMINDER | CONFIRMATION | CANCELLATION | QUEUE_UPDATE */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean success;

    /** Error message if success == false */
    @Column(length = 500)
    private String errorMessage;
}
