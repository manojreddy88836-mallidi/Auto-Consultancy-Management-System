package com.autoconsultancy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalCustomers;
    private long totalWorkers;
    private long totalApplications;
    private long pendingApplications;
    private long underReviewApplications;
    private long approvedApplications;
    private long rejectedApplications;
    private long completedApplications;
    private long bikesUnderFinance;
    private long bikesWithoutFinance;
    private long financeVerificationPending;
    private long documentsUnderReview;
    private long newApplicationsThisMonth;
    private long newCustomersThisMonth;
}
