package com.autoconsultancy.dto.response;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class WorkerDashboardStats {
    // Existing application stats
    private long totalAssigned;
    private long pendingVerification;
    private long completedApplications;
    private long documentsToReview;
    private long financeToVerify;

    // New field-task stats
    private long totalTasks;
    private long tasksPending;
    private long tasksInProgress;
    private long tasksCompleted;
    private long repairJobs;
    private long collectionTasks;
    private long visitTasks;
    private long recoveryTasks;
}
