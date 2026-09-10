package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerDashboardStats {
    private long totalApplications;
    private long draftApplications;
    private long submittedApplications;
    private long pendingApplications;
    private long approvedApplications;
    private long rejectedApplications;
    private long completedApplications;
}
