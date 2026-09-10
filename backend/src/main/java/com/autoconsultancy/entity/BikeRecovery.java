package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "bike_recoveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeRecovery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_task_id", nullable = false)
    private WorkerTask workerTask;

    @Column(precision = 12, scale = 2) private BigDecimal outstandingAmount;
    @Column(columnDefinition = "TEXT") private String reason;
    private String authorizationNumber;
    private LocalDate recoveryDate;
    private String bikeCondition;
    private Integer currentMileage;
    @Column(columnDefinition = "TEXT") private String existingDamage;
    private boolean accessoriesReceived;
    private boolean keysReceived;
    private boolean documentsReceived;
    @Column(columnDefinition = "TEXT") private String workerRecoveryNotes;
    private boolean customerAcknowledged;
}