package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BikeInventoryImageResponse {
    private Long id;
    private Long bikeInventoryId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;
    private boolean primary;
    /** Relative URL served by the backend, e.g. /api/bikes/{id}/images/{imageId}/file */
    private String imageUrl;
    private LocalDateTime createdAt;
}
