package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BikeImageResponse {
    private Long id;
    private Long bikeModelId;
    private String fileName;
    private String originalFileName;
    private boolean primary;
    private Long fileSize;
    private String mimeType;
    private String imageUrl;   // URL to fetch the image file
    private LocalDateTime createdAt;
}
