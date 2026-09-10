package com.autoconsultancy.service;

import com.autoconsultancy.entity.Application.ApplicationStatus;
import com.autoconsultancy.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;

    /**
     * Monthly applications for the current year.
     * Returns list of {month, count} maps with all 12 months populated (0 if none).
     */
    public List<Map<String, Object>> getApplicationsByMonth() {
        List<Object[]> raw = applicationRepository.countByMonth();

        // Build a map: monthNum -> count
        Map<Integer, Long> byMonth = new LinkedHashMap<>();
        for (Object[] row : raw) {
            int monthNum = ((Number) row[0]).intValue();
            long count   = ((Number) row[1]).longValue();
            byMonth.put(monthNum, count);
        }

        // Fill all 12 months so the chart always has a full year
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", Month.of(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("count", byMonth.getOrDefault(i, 0L));
            result.add(entry);
        }
        return result;
    }

    /**
     * Application counts grouped by status.
     * Returns list of {name, value} maps.
     */
    public List<Map<String, Object>> getApplicationsByStatus() {
        List<Object[]> raw = applicationRepository.countByStatusGrouped();
        List<Map<String, Object>> result = new ArrayList<>();

        // Label mapping for display
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("DRAFT",                 "Draft");
        labels.put("SUBMITTED",             "Submitted");
        labels.put("UNDER_REVIEW",          "Under Review");
        labels.put("DOCUMENT_VERIFICATION","Doc Verification");
        labels.put("FINANCE_VERIFICATION",  "Finance Verification");
        labels.put("WORKER_ASSIGNED",       "Worker Assigned");
        labels.put("APPROVED",              "Approved");
        labels.put("REJECTED",              "Rejected");
        labels.put("COMPLETED",             "Completed");

        // Index raw results
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Object[] row : raw) {
            String status = ((ApplicationStatus) row[0]).name();
            long   count  = ((Number) row[1]).longValue();
            countMap.put(status, count);
        }

        // Emit in canonical order, skip zeros
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            long count = countMap.getOrDefault(entry.getKey(), 0L);
            if (count > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name",  entry.getValue());
                item.put("value", count);
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Finance breakdown: underFinance=true vs false.
     * Returns {underFinance, notUnderFinance} map.
     */
    public Map<String, Long> getFinanceStats() {
        List<Object[]> raw = applicationRepository.countByFinanceStatus();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("underFinance",    0L);
        result.put("notUnderFinance", 0L);
        for (Object[] row : raw) {
            boolean under = (Boolean) row[0];
            long    count = ((Number) row[1]).longValue();
            result.put(under ? "underFinance" : "notUnderFinance", count);
        }
        return result;
    }

    /**
     * Top 7 manufacturers by application count.
     * Returns list of {name, count} maps.
     */
    public List<Map<String, Object>> getManufacturerStats() {
        List<Object[]> raw = applicationRepository.countByManufacturer(PageRequest.of(0, 7));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name",  row[0]);
            item.put("count", ((Number) row[1]).longValue());
            result.add(item);
        }
        return result;
    }

    /**
     * Top 10 workers by assigned application count.
     * Returns list of {name, count} maps.
     */
    public List<Map<String, Object>> getApplicationsByWorker() {
        List<Object[]> raw = applicationRepository.countByWorker(PageRequest.of(0, 10));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name",  row[0] + " " + row[1]);
            item.put("count", ((Number) row[2]).longValue());
            result.add(item);
        }
        return result;
    }
}
