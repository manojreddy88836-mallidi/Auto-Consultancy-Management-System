package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bike_inventory_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BikeInventoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_inventory_id", nullable = false)
    private BikeInventory bikeInventory;

    /** Relative path, e.g. /uploads/bike-inventory/{inventoryId}/{uuid}.jpg */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
