package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "bike_recoveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeRecovery {

    @Id
    private Long id;

    @DBRef
    private WorkerTask workerTask;

    private BigDecimal outstandingAmount;
    private String reason;
    private String authorizationNumber;
    private LocalDate recoveryDate;
    private String bikeCondition;
    private Integer currentMileage;
    private String existingDamage;
    private boolean accessoriesReceived;
    private boolean keysReceived;
    private boolean documentsReceived;
    private String workerRecoveryNotes;
    private boolean customerAcknowledged;
}