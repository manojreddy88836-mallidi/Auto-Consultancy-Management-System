package com.autoconsultancy.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class WorkerTaskResponse {
    private Long id;
    private String taskType;
    private String status;
    private String priority;
    private String title;
    private String description;
    private String adminNotes;
    private String workerNotes;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Worker
    private Long workerId;
    private String workerName;

    // Customer
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Bike
    private Long bikeInventoryId;
    private String bikeCode;
    private String bikeModel;
    private String registrationNumber;

    // Application
    private Long applicationId;
    private String applicationNumber;

    // Summary fields for lists
    private BigDecimal amountDue;
    private BigDecimal amountCollected;
    private BigDecimal outstandingAmount;
}
