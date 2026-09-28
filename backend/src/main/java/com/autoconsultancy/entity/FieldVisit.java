package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "field_visits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FieldVisit {

    @Id
    private Long id;

    @DBRef
    private WorkerTask workerTask;

    private LocalDate visitDate;
    private boolean customerContacted;
    private BigDecimal amountCollected;
    private LocalDate promiseToPayDate;
    private BigDecimal promiseAmount;
    private String customerResponse;
    private String locationNotes;
    private String outcomeNotes;
}