package com.autoconsultancy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    private Long id;

    @DBRef
    @JsonIgnore
    private Application application;

    @DBRef
    private User uploadedByUser;

    private String documentType;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String status;
    private String remarks;

    @CreatedDate
    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    @DBRef
    private User reviewedBy;
}
