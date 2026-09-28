package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmiPaymentRequest {

    @NotNull(message = "Finance detail ID is required")
    private Long financeDetailId;

    @NotNull(message = "Installment number is required")
    @Min(value = 1, message = "Installment number must be at least 1")
    @Max(value = 360, message = "Installment number must not exceed 360")
    private Integer installmentNumber;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "1.00", message = "Amount paid must be at least ₹1")
    @DecimalMax(value = "10000000.00", message = "Amount paid exceeds maximum allowed")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal amountPaid;

    @Size(max = 50, message = "Payment mode must not exceed 50 characters")
    private String paymentMode;

    @Size(max = 200, message = "Reference number must not exceed 200 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_/]*$", message = "Reference number contains invalid characters")
    private String referenceNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
