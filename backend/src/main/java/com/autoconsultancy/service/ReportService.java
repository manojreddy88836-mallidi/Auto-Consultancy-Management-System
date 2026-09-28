package com.autoconsultancy.service;

import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import com.autoconsultancy.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService — all analytics use MongoDB aggregation pipelines via MongoTemplate.
 * No findAll() calls — data is aggregated at the database level.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Applications by month (current year) — uses $match + $group aggregation.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicationsByMonth() {
        int currentYear = java.time.LocalDate.now().getYear();

        // Use aggregation: $project year/month from createdAt, $match year, $group by month
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.project()
                        .andExpression("year(createdAt)").as("yr")
                        .andExpression("month(createdAt)").as("mo"),
                Aggregation.match(Criteria.where("yr").is(currentYear)),
                Aggregation.group("mo").count().as("count"),
                Aggregation.project("count").and("_id").as("month")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "applications", Map.class);
        Map<Integer, Long> byMonth = new LinkedHashMap<>();
        for (Map doc : results.getMappedResults()) {
            Number mo = (Number) doc.get("month");
            Number cnt = (Number) doc.get("count");
            if (mo != null && cnt != null) byMonth.put(mo.intValue(), cnt.longValue());
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

    /**
     * Applications by status — uses per-status countByStatus (single indexed count per status).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicationsByStatus() {
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
            long count = applicationRepository.countByStatus(ApplicationStatus.valueOf(entry.getKey()));
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
     * Finance vs non-finance stats — uses MongoDB aggregation $group on financeDetail.underFinance.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getFinanceStats() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("financeDetail.underFinance").count().as("count")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "applications", Map.class);

        long underFinance = 0L;
        long notUnderFinance = 0L;
        for (Map doc : results.getMappedResults()) {
            Object id = doc.get("_id");
            Number cnt = (Number) doc.get("count");
            if (cnt == null) continue;
            if (Boolean.TRUE.equals(id)) underFinance = cnt.longValue();
            else notUnderFinance += cnt.longValue();
        }

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("underFinance",    underFinance);
        result.put("notUnderFinance", notUnderFinance);
        return result;
    }

    /**
     * Applications by manufacturer — uses $group aggregation on embedded manufacturer.name.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getManufacturerStats() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("bikeDetail.manufacturer.name").exists(true).ne(null)),
                Aggregation.group("bikeDetail.manufacturer.name").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count"),
                Aggregation.limit(7)
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "applications", Map.class);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map doc : results.getMappedResults()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", doc.get("_id"));
            Number cnt = (Number) doc.get("count");
            item.put("count", cnt != null ? cnt.longValue() : 0L);
            result.add(item);
        }
        return result;
    }

    /**
     * Applications by assigned worker — uses $group aggregation on embedded worker name fields.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicationsByWorker() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("workerAssignment.worker.user.firstName").exists(true).ne(null)),
                Aggregation.project()
                        .andExpression("concat(workerAssignment.worker.user.firstName, ' ', workerAssignment.worker.user.lastName)")
                        .as("workerName"),
                Aggregation.group("workerName").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count"),
                Aggregation.limit(10)
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "applications", Map.class);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map doc : results.getMappedResults()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", doc.get("_id"));
            Number cnt = (Number) doc.get("count");
            item.put("count", cnt != null ? cnt.longValue() : 0L);
            result.add(item);
        }
        return result;
    }
}
