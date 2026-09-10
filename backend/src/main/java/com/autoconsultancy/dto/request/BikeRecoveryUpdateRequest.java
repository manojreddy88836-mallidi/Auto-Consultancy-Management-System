package com.autoconsultancy.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BikeRecoveryUpdateRequest {
    private LocalDate recoveryDate;
    private String bikeCondition;
    private Integer currentMileage;
    private String existingDamage;
    private boolean accessoriesReceived;
    private boolean keysReceived;
    private boolean documentsReceived;
    private String workerRecoveryNotes;
    private boolean customerAcknowledged;
    private String newStatus;
}
