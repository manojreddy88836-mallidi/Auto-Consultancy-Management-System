package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "service_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceJob {
    public enum PaymentStatus { PENDING, PARTIAL, PAID }

    @Id
    private Long id;

    @DBRef
    private WorkerTask workerTask;

    private String registrationNumber;
    private String taskDescription;
    private String partsUsed;
    private BigDecimal labourCharge;
    private BigDecimal partsCharge;
    private BigDecimal totalAmount;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDate serviceDate;
    private LocalDate completionDate;
    private String notes;
}