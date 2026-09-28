package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "worker_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAssignment {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Application application;

    @DBRef
    private Worker worker;

    @DBRef
    private User assignedBy;

    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private String notes;

    @Builder.Default
    private boolean active = true;
}
