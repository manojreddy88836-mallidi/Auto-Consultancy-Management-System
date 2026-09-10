package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emi_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_detail_id", nullable = false)
    @JsonIgnore
    private FinanceDetail financeDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonIgnore
    private Application application;

    // Which installment number this payment covers (1-based)
    private Integer installmentNumber;

    private LocalDate paymentDate;

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    // CASH / UPI / BANK_TRANSFER / CARD / OTHER
    private String paymentMode;

    private String referenceNumber;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    @JsonIgnore
    private User recordedBy;

    @CreationTimestamp
    private LocalDateTime recordedAt;
}
