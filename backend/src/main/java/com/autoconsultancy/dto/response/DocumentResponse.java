package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private Long applicationId;
    private String documentType;
    private String originalFileName;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String status;
    private String remarks;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
}
