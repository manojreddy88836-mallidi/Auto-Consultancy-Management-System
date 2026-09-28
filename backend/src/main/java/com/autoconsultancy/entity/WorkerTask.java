package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "worker_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerTask {
    public enum TaskType   { REPAIR, COLLECTION, VISIT, RECOVERY }
    public enum TaskStatus {
        ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED,
        VISITED, PAYMENT_COLLECTED, PROMISED_TO_PAY, CUSTOMER_UNAVAILABLE, ESCALATED,
        RECOVERY_ASSIGNED, CUSTOMER_CONTACTED, RECOVERY_SCHEDULED, BIKE_RECOVERED, CUSTOMER_PAID
    }
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    @Id
    private Long id;

    private TaskType taskType;

    @Builder.Default
    private TaskStatus status = TaskStatus.ASSIGNED;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private String title;
    private String description;

    @DBRef
    private Worker worker;

    @DBRef
    private Customer customer;

    @DBRef
    private BikeInventory bikeInventory;

    @DBRef
    private Application application;

    @DBRef
    private User createdBy;

    private LocalDate dueDate;
    private LocalDateTime completedAt;

    private String adminNotes;
    private String workerNotes;

    @DBRef
    private ServiceJob serviceJob;

    @DBRef
    private PaymentCollection paymentCollection;

    @DBRef
    private FieldVisit fieldVisit;

    @DBRef
    private BikeRecovery bikeRecovery;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}