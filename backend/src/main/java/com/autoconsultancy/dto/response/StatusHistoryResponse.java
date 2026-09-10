package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StatusHistoryResponse {
    private Long id;
    private String previousStatus;
    private String newStatus;
    private String changedByName;
    private String remarks;
    private LocalDateTime changedAt;
}
