package com.autoconsultancy.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WorkerTaskRequest {
    private String taskType;      // REPAIR | COLLECTION | VISIT | RECOVERY
    private String title;
    private String description;
    private String priority;      // LOW | NORMAL | HIGH | URGENT
    private Long workerId;
    private Long customerId;
    private Long bikeInventoryId;
    private Long applicationId;
    private LocalDate dueDate;
    private String adminNotes;

    // For COLLECTION task
    private Long financeDetailId;
    private BigDecimal amountDue;

    // For RECOVERY task
    private BigDecimal outstandingAmount;
    private String reason;
    private String authorizationNumber;
}
