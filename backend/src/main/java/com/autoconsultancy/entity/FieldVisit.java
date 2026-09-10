package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "field_visits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FieldVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_task_id", nullable = false)
    private WorkerTask workerTask;

    private LocalDate visitDate;
    private boolean customerContacted;
    @Column(precision = 12, scale = 2) private BigDecimal amountCollected;
    private LocalDate promiseToPayDate;
    @Column(precision = 12, scale = 2) private BigDecimal promiseAmount;
    @Column(columnDefinition = "TEXT") private String customerResponse;
    @Column(columnDefinition = "TEXT") private String locationNotes;
    @Column(columnDefinition = "TEXT") private String outcomeNotes;
}