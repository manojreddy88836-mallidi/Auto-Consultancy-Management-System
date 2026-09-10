package com.autoconsultancy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiPaymentResponse {

    private Long id;
    private Long financeDetailId;
    private Long applicationId;
    private String applicationNumber;

    private Integer installmentNumber;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private String paymentMode;
    private String referenceNumber;
    private String notes;

    private String recordedByName;
    private LocalDateTime recordedAt;
}
