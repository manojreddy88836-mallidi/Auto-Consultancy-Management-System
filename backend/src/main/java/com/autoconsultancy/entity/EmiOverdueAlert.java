package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "emi_overdue_alerts")
@CompoundIndex(name = "uk_emi_overdue_alert", def = "{'financeDetail': 1, 'overdueStatus': 1, 'alertMonth': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiOverdueAlert {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private FinanceDetail financeDetail;

    private String overdueStatus;
    private String alertMonth;

    @CreatedDate
    private LocalDateTime createdAt;
}
