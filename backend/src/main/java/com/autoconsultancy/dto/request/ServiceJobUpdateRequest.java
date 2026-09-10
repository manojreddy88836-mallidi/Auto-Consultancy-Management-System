package com.autoconsultancy.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ServiceJobUpdateRequest {
    private String registrationNumber;
    private String taskDescription;
    private String partsUsed;
    private BigDecimal labourCharge;
    private BigDecimal partsCharge;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private LocalDate serviceDate;
    private LocalDate completionDate;
    private String notes;
}
