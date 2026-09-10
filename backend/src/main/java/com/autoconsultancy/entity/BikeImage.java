package com.autoconsultancy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bike_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_model_id", nullable = false)
    private BikeModel bikeModel;

    @Column(nullable = false)
    private String fileName;

    private String originalFileName;

    @Column(nullable = false)
    private String filePath;

    @Builder.Default
    @Column(name = "is_primary")
    private boolean primary = false;

    private Long fileSize;
    private String mimeType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
