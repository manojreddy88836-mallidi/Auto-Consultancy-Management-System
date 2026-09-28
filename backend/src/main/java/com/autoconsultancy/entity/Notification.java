package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private Long id;

    @JsonIgnore
    @DBRef
    private User user;

    private String title;
    private String message;
    private String type;
    
    @Builder.Default
    private boolean read = false;
    
    private Long relatedApplicationId;

    @CreatedDate
    private LocalDateTime createdAt;
}
