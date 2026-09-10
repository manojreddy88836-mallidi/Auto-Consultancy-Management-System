package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String remarks;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private BikeDetail bikeDetail;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private FinanceDetail financeDetail;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ApplicationStatusHistory> statusHistory = new ArrayList<>();

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private WorkerAssignment workerAssignment;

    public enum ApplicationStatus {
        DRAFT, SUBMITTED, UNDER_REVIEW, DOCUMENT_VERIFICATION, FINANCE_VERIFICATION, WORKER_ASSIGNED, APPROVED, REJECTED, COMPLETED
    }
}
