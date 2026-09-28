package com.autoconsultancy.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "bike_inventory_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeInventoryImage {

    @Id
    private Long id;

    @DBRef
    private BikeInventory bikeInventory;

    private String filePath;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;

    @Builder.Default
    private boolean primary = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
