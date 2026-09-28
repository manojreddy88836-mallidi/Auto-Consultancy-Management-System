package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    private Long id;

    @Indexed(unique = true)
    private String applicationNumber;

    @DBRef
    private Customer customer;

    private ApplicationStatus status;

    private String remarks;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    @DBRef
    private BikeDetail bikeDetail;

    @DBRef
    private FinanceDetail financeDetail;

    @DBRef
    @Builder.Default
    private List<com.autoconsultancy.entity.Document> documents = new ArrayList<>();

    @DBRef
    @Builder.Default
    private List<ApplicationStatusHistory> statusHistory = new ArrayList<>();

    @DBRef
    private WorkerAssignment workerAssignment;

    public enum ApplicationStatus {
        DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENT_VERIFICATION, FINANCE_VERIFICATION, WORKER_ASSIGNED, APPROVED, REJECTED, COMPLETED
    }
}
