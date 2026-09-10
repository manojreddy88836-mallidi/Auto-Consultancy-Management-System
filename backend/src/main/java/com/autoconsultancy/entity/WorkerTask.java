package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerTask {
    public enum TaskType   { REPAIR, COLLECTION, VISIT, RECOVERY }
    public enum TaskStatus {
        ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED,
        VISITED, PAYMENT_COLLECTED, PROMISED_TO_PAY, CUSTOMER_UNAVAILABLE, ESCALATED,
        RECOVERY_ASSIGNED, CUSTOMER_CONTACTED, RECOVERY_SCHEDULED, BIKE_RECOVERED, CUSTOMER_PAID
    }
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TaskType taskType;

    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private TaskStatus status = TaskStatus.ASSIGNED;

    @Enumerated(EnumType.STRING) @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bike_inventory_id")
    private BikeInventory bikeInventory;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "application_id")
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private LocalDate dueDate;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT") private String adminNotes;
    @Column(columnDefinition = "TEXT") private String workerNotes;

    @OneToOne(mappedBy = "workerTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ServiceJob serviceJob;

    @OneToOne(mappedBy = "workerTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentCollection paymentCollection;

    @OneToOne(mappedBy = "workerTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FieldVisit fieldVisit;

    @OneToOne(mappedBy = "workerTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BikeRecovery bikeRecovery;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}