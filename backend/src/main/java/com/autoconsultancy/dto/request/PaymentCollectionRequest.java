package com.autoconsultancy.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentCollectionRequest {
    private BigDecimal amountCollected;
    private String paymentMethod;
    private LocalDate collectionDate;
    private String receiptNumber;
    private String upiReference;
    private String notes;
}
