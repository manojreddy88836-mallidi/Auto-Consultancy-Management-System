package com.autoconsultancy.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FieldVisitUpdateRequest {
    private LocalDate visitDate;
    private boolean customerContacted;
    private BigDecimal amountCollected;
    private LocalDate promiseToPayDate;
    private BigDecimal promiseAmount;
    private String customerResponse;
    private String locationNotes;
    private String outcomeNotes;
    private String newStatus;
}
