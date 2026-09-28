package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "application_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusHistory {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Application application;

    private String previousStatus;
    private String newStatus;

    @DBRef
    private User changedBy;

    private String remarks;

    @CreatedDate
    private LocalDateTime changedAt;
}
