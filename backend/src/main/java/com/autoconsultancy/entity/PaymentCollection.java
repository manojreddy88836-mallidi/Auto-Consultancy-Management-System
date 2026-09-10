package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "payment_collections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentCollection {
    public enum PaymentMethod { CASH, UPI, BANK_TRANSFER, OTHER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_task_id", nullable = false)
    private WorkerTask workerTask;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "finance_detail_id")
    private FinanceDetail financeDetail;

    @Column(precision = 12, scale = 2) private BigDecimal amountDue;
    @Column(precision = 12, scale = 2) private BigDecimal amountCollected;

    @Enumerated(EnumType.STRING) @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    private LocalDate collectionDate;
    private String receiptNumber;
    private String upiReference;
    @Column(columnDefinition = "TEXT") private String notes;
    private boolean verifiedByAdmin;
    @CreationTimestamp private LocalDateTime createdAt;
}