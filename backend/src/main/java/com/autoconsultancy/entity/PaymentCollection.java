package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "payment_collections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentCollection {
    public enum PaymentMethod { CASH, UPI, BANK_TRANSFER, OTHER }

    @Id
    private Long id;

    @DBRef
    private WorkerTask workerTask;

    @DBRef
    private FinanceDetail financeDetail;

    private BigDecimal amountDue;
    private BigDecimal amountCollected;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    private LocalDate collectionDate;
    private String receiptNumber;
    private String upiReference;
    private String notes;
    private boolean verifiedByAdmin;

    @CreatedDate
    private LocalDateTime createdAt;
}