package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "service_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceJob {
    public enum PaymentStatus { PENDING, PARTIAL, PAID }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_task_id", nullable = false)
    private WorkerTask workerTask;

    private String registrationNumber;
    @Column(columnDefinition = "TEXT") private String taskDescription;
    @Column(columnDefinition = "TEXT") private String partsUsed;
    @Column(precision = 12, scale = 2) private BigDecimal labourCharge;
    @Column(precision = 12, scale = 2) private BigDecimal partsCharge;
    @Column(precision = 12, scale = 2) private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING) @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDate serviceDate;
    private LocalDate completionDate;
    @Column(columnDefinition = "TEXT") private String notes;
}