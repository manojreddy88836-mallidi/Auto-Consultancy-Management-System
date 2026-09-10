package com.autoconsultancy.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmiPaymentRequest {

    private Long financeDetailId;           // required

    private Integer installmentNumber;       // which EMI installment (1-based)

    private LocalDate paymentDate;           // date the customer paid

    private BigDecimal amountPaid;           // actual amount paid

    // CASH / UPI / BANK_TRANSFER / CARD / OTHER
    private String paymentMode;

    private String referenceNumber;          // optional transaction ref

    private String notes;                    // optional admin notes
}
