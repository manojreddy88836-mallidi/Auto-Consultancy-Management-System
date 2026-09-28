package com.autoconsultancy.service;

import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import com.autoconsultancy.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;

    public List<Map<String, Object>> getApplicationsByMonth() {
        List<Application> apps = applicationRepository.findAll();
        int currentYear = LocalDateTime.now().getYear();

        Map<Integer, Long> byMonth = new LinkedHashMap<>();
        for (Application a : apps) {
            if (a.getCreatedAt() != null && a.getCreatedAt().getYear() == currentYear) {
                int m = a.getCreatedAt().getMonthValue();
                byMonth.put(m, byMonth.getOrDefault(m, 0L) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", Month.of(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("count", byMonth.getOrDefault(i, 0L));
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> getApplicationsByStatus() {
        List<Application> apps = applicationRepository.findAll();
        Map<ApplicationStatus, Long> statusCounts = apps.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("DRAFT",                 "Draft");
        labels.put("SUBMITTED",             "Submitted");
        labels.put("UNDER_REVIEW",          "Under Review");
        labels.put("DOCUMENT_VERIFICATION", "Doc Verification");
        labels.put("FINANCE_VERIFICATION",  "Finance Verification");
        labels.put("WORKER_ASSIGNED",       "Worker Assigned");
        labels.put("APPROVED",              "Approved");
        labels.put("REJECTED",              "Rejected");
        labels.put("COMPLETED",             "Completed");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            ApplicationStatus status = ApplicationStatus.valueOf(entry.getKey());
            long count = statusCounts.getOrDefault(status, 0L);
            if (count > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name",  entry.getValue());
                item.put("value", count);
                result.add(item);
            }
        }
        return result;
    }

    public Map<String, Long> getFinanceStats() {
        List<Application> apps = applicationRepository.findAll();
        long underFinance = apps.stream().filter(a -> a.getFinanceDetail() != null && a.getFinanceDetail().isUnderFinance()).count();
        long notUnderFinance = apps.size() - underFinance;

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("underFinance",    underFinance);
        result.put("notUnderFinance", notUnderFinance);
        return result;
    }

    public List<Map<String, Object>> getManufacturerStats() {
        List<Application> apps = applicationRepository.findAll();
        Map<String, Long> map = new HashMap<>();
        for (Application a : apps) {
            if (a.getBikeDetail() != null && a.getBikeDetail().getManufacturer() != null && a.getBikeDetail().getManufacturer().getName() != null) {
                String mName = a.getBikeDetail().getManufacturer().getName();
                map.put(mName, map.getOrDefault(mName, 0L) + 1);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    result.add(item);
                });
        return result;
    }

    public List<Map<String, Object>> getApplicationsByWorker() {
        List<Application> apps = applicationRepository.findAll();
        Map<String, Long> map = new HashMap<>();
        for (Application a : apps) {
            if (a.getWorkerAssignment() != null && a.getWorkerAssignment().getWorker() != null && a.getWorkerAssignment().getWorker().getUser() != null) {
                String name = a.getWorkerAssignment().getWorker().getUser().getFirstName() + " " + a.getWorkerAssignment().getWorker().getUser().getLastName();
                map.put(name, map.getOrDefault(name, 0L) + 1);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    result.add(item);
                });
        return result;
    }
}
