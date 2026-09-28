package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "emi_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiPayment {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private FinanceDetail financeDetail;

    @DBRef
    @JsonIgnore
    private Application application;

    private Integer installmentNumber;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private String paymentMode;
    private String referenceNumber;
    private String notes;

    @DBRef
    @JsonIgnore
    private User recordedBy;

    @CreatedDate
    private LocalDateTime recordedAt;
}
