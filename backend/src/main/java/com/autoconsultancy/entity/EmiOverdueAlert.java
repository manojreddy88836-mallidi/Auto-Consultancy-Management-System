package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Prevents duplicate overdue notifications.
 * One row per (financeDetailId, overdueStatus, alertMonth).
 * If a row exists for this month+status, no new notification is sent.
 */
@Entity
@Table(name = "emi_overdue_alerts", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_emi_overdue_alert",
        columnNames = {"finance_detail_id", "overdue_status", "alert_month"}
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiOverdueAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_detail_id", nullable = false)
    @JsonIgnore
    private FinanceDetail financeDetail;

    // TWO_MONTHS or CRITICAL
    private String overdueStatus;

    // "YYYY-MM" — one alert per status per month max
    private String alertMonth;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
