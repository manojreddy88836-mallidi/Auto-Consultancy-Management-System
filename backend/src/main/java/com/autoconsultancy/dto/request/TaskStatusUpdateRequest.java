package com.autoconsultancy.dto.request;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {
    private String status;
    private String workerNotes;
}
