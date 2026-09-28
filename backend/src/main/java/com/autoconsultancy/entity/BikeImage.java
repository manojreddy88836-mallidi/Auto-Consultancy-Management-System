package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "bike_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeImage {

    @Id
    private Long id;

    @DBRef
    private BikeModel bikeModel;

    private String fileName;
    private String originalFileName;
    private String filePath;

    @Builder.Default
    private boolean primary = false;

    private Long fileSize;
    private String mimeType;

    @CreatedDate
    private LocalDateTime createdAt;
}
