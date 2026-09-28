package com.autoconsultancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.DBRef;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * READ-ONLY full comparison between MySQL auto_consultancy and MongoDB shop_management.
 * Produces MISSING, EXTRA, MISMATCH lists per collection.
 * No data is modified.
 */
@SpringBootTest
public class FullSyncComparisonTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Connection getMysql() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root");
    }

    // ── helper ──────────────────────────────────────────────────────────────
    private List<Document> mongoAll(String coll) {
        List<Document> list = new ArrayList<>();
        mongoTemplate.getCollection(coll).find().into(list);
        return list;
    }

    private Object resolveId(Document doc) {
        Object id = doc.get("_id");
        if (id instanceof org.bson.types.ObjectId oid) return oid.toHexString();
        return id;
    }

    private void printSectionHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }

    private void printRow(String label, int sql, int mongo, List<Long> missing, List<Long> extra) {
        String status = (sql == mongo && missing.isEmpty() && extra.isEmpty()) ? "MATCH" :
                        (missing.isEmpty() && extra.isEmpty()) ? "COUNT_OK" : "MISMATCH";
        System.out.printf("%-30s | SQL=%4d | Mongo=%4d | Missing=%3d | Extra=%3d | %s%n",
                label, sql, mongo, missing.size(), extra.size(), status);
        if (!missing.isEmpty()) System.out.println("   MISSING IDs: " + missing);
        if (!extra.isEmpty())   System.out.println("   EXTRA IDs:   " + extra);
    }

    // ── MAIN TEST ────────────────────────────────────────────────────────────
    @Test
    public void runFullSyncComparison() throws Exception {
        printSectionHeader("FULL SYNC COMPARISON: MySQL vs MongoDB");
        try (Connection conn = getMysql()) {
            compareUsers(conn);
            compareCustomers(conn);
            compareWorkers(conn);
            compareManufacturers(conn);
            compareBikeModels(conn);
            compareBikeVariants(conn);
            compareManufacturingYears(conn);
            compareApplications(conn);
            compareFinanceDetails(conn);
            compareEmiPayments(conn);
            compareBikeInventory(conn);
            compareBikeInventoryImages(conn);
            compareBikeDetails(conn);
            compareDocuments(conn);
            compareNotifications(conn);
            compareEmiOverdueAlerts(conn);
            compareBikeOffers(conn);
            compareWorkerTasks(conn);
            compareServiceJobs(conn);
            compareAppStatusHistory(conn);
            checkItemsAndSales();
        }
        printSectionHeader("COMPARISON COMPLETE");
    }

    // ── USERS ────────────────────────────────────────────────────────────────
    private void compareUsers(Connection conn) throws SQLException {
        printSectionHeader("USERS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        Map<Long, String> sqlEmails = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, email, role FROM users ORDER BY id")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                sqlIds.add(id);
                sqlEmails.put(id, rs.getString("email"));
                System.out.printf("  SQL User id=%-3d email=%-45s role=%s%n",
                        id, rs.getString("email"), rs.getString("role"));
            }
        }

        Map<Object, Document> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("users")) {
            mongoMap.put(resolveId(d), d);
        }
        System.out.println("  --- MongoDB Users ---");
        for (Map.Entry<Object, Document> e : mongoMap.entrySet()) {
            System.out.printf("  Mongo User id=%-5s email=%-45s role=%s%n",
                    e.getKey(), e.getValue().getString("email"), e.getValue().getString("role"));
        }

        List<Long> missing = sqlIds.stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Object> extra = mongoMap.keySet().stream().filter(k -> {
            try { return !sqlIds.contains(Long.parseLong(k.toString())); } catch (NumberFormatException ex) { return true; }
        }).collect(Collectors.toList());

        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlIds.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL user IDs: " + missing);
        if (!extra.isEmpty())   System.out.println("  EXTRA Mongo user IDs: " + extra);
    }

    // ── CUSTOMERS ─────────────────────────────────────────────────────────────
    private void compareCustomers(Connection conn) throws SQLException {
        printSectionHeader("CUSTOMERS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT c.id, u.email FROM customers c JOIN users u ON c.id = u.id ORDER BY c.id")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                sqlIds.add(id);
                System.out.printf("  SQL Customer id=%-3d email=%s%n", id, rs.getString("email"));
            }
        }
        List<Document> mongoDocs = mongoAll("customers");
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoDocs) {
            Object raw = d.get("_id");
            try { mongoIds.add(Long.parseLong(raw.toString())); } catch (Exception e) { /* skip */ }
            System.out.printf("  Mongo Customer id=%-5s userRef=%s%n", raw, d.get("user"));
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlIds.size(), mongoIds.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL customer IDs (need recovery): " + missing);
        if (!extra.isEmpty())   System.out.println("  EXTRA Mongo customer IDs (candidates for removal): " + extra);
    }

    // ── WORKERS ──────────────────────────────────────────────────────────────
    private void compareWorkers(Connection conn) throws SQLException {
        printSectionHeader("WORKERS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT w.id, u.email, w.employee_id FROM workers w JOIN users u ON w.id = u.id ORDER BY w.id")) {
            while (rs.next()) { sqlIds.add(rs.getLong("id")); System.out.printf("  SQL Worker id=%-3d email=%s emp=%s%n", rs.getLong("id"), rs.getString("email"), rs.getString("employee_id")); }
        }
        List<Document> mongoDocs = mongoAll("workers");
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoDocs) { try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */} }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n", sqlIds.size(), mongoIds.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING: " + missing);
        if (!extra.isEmpty())   System.out.println("  EXTRA: " + extra);
    }

    // ── MANUFACTURERS ─────────────────────────────────────────────────────────
    private void compareManufacturers(Connection conn) throws SQLException {
        printSectionHeader("MANUFACTURERS");
        Map<Long, String> sqlMap = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM manufacturers ORDER BY id")) {
            while (rs.next()) sqlMap.put(rs.getLong("id"), rs.getString("name"));
        }

        Map<Long, String> mongoById = new LinkedHashMap<>();
        Map<String, Long> mongoByName = new LinkedHashMap<>();
        for (Document d : mongoAll("manufacturers")) {
            Object rawId = d.get("_id");
            try {
                long lid = Long.parseLong(rawId.toString());
                String name = d.getString("name");
                mongoById.put(lid, name);
                if (name != null) mongoByName.put(name.toLowerCase().trim(), lid);
            } catch (Exception e) {/* skip */}
        }

        List<Long> missingById = new ArrayList<>();
        List<Long> nameMismatch = new ArrayList<>();
        for (Map.Entry<Long, String> e : sqlMap.entrySet()) {
            long sqlId = e.getKey();
            String sqlName = e.getValue();
            String mongoName = mongoById.get(sqlId);
            if (mongoName == null) {
                // Not found by ID — check by name
                Long mongoId = mongoByName.get(sqlName.toLowerCase().trim());
                if (mongoId == null) {
                    missingById.add(sqlId);
                    System.out.printf("  MISSING MANUFACTURER: SQL id=%d name=%s%n", sqlId, sqlName);
                } else {
                    nameMismatch.add(sqlId);
                    System.out.printf("  ID MISMATCH: SQL id=%d (%s) exists in Mongo as id=%d%n", sqlId, sqlName, mongoId);
                }
            } else if (!mongoName.equalsIgnoreCase(sqlName)) {
                nameMismatch.add(sqlId);
                System.out.printf("  NAME MISMATCH: SQL id=%d=%s but Mongo id=%d=%s%n", sqlId, sqlName, sqlId, mongoName);
            }
        }
        List<Long> extra = mongoById.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        if (!extra.isEmpty()) {
            System.out.println("  EXTRA Mongo manufacturer IDs (no corresponding SQL record):");
            for (Long eid : extra) System.out.printf("    id=%d name=%s%n", eid, mongoById.get(eid));
        }
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | MissingById=%d | IDMismatch=%d | Extra=%d%n",
                sqlMap.size(), mongoById.size(), missingById.size(), nameMismatch.size(), extra.size());
    }

    // ── BIKE MODELS ───────────────────────────────────────────────────────────
    private void compareBikeModels(Connection conn) throws SQLException {
        printSectionHeader("BIKE MODELS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, model_name, manufacturer_id FROM bike_models ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_models")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        System.out.printf("Summary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n", sqlIds.size(), mongoIds.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING IDs: " + missing.subList(0, Math.min(10, missing.size())) + (missing.size()>10?" ...":""));
        if (!extra.isEmpty())   System.out.println("  EXTRA IDs (first 10): " + extra.subList(0, Math.min(10, extra.size())) + (extra.size()>10?" ...":""));
    }

    // ── BIKE VARIANTS ────────────────────────────────────────────────────────
    private void compareBikeVariants(Connection conn) throws SQLException {
        printSectionHeader("BIKE VARIANTS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM bike_variants ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_variants")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        System.out.printf("Summary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n", sqlIds.size(), mongoIds.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING IDs: " + missing.subList(0, Math.min(10, missing.size())) + (missing.size()>10?" ...":""));
        if (!extra.isEmpty())   System.out.println("  EXTRA IDs (first 10): " + extra.subList(0, Math.min(10, extra.size())) + (extra.size()>10?" ...":""));
    }

    // ── MANUFACTURING YEARS ───────────────────────────────────────────────────
    private void compareManufacturingYears(Connection conn) throws SQLException {
        printSectionHeader("MANUFACTURING YEARS");
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM manufacturing_years ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("manufacturing_years")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        System.out.printf("Summary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n", sqlIds.size(), mongoIds.size(), missing.size(), extra.size());
        if (!extra.isEmpty()) System.out.println("  EXTRA IDs (first 10): " + extra.subList(0, Math.min(10, extra.size())) + (extra.size()>10?" ...":""));
    }

    // ── APPLICATIONS ──────────────────────────────────────────────────────────
    private void compareApplications(Connection conn) throws SQLException {
        printSectionHeader("APPLICATIONS");
        Map<Long, String> sqlMap = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, application_number, customer_id, status FROM applications ORDER BY id")) {
            while (rs.next()) {
                sqlMap.put(rs.getLong("id"), rs.getString("application_number"));
                System.out.printf("  SQL App id=%-3d appNo=%-20s custId=%-3d status=%s%n",
                        rs.getLong("id"), rs.getString("application_number"),
                        rs.getLong("customer_id"), rs.getString("status"));
            }
        }
        Map<Long, String> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("applications")) {
            try {
                Long id = Long.parseLong(d.get("_id").toString());
                String appNo = d.getString("applicationNumber");
                mongoMap.put(id, appNo);
                System.out.printf("  Mongo App id=%-3d appNo=%-20s custRef=%s%n", id, appNo, d.get("customer"));
            } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlMap.keySet().stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Long> extra = mongoMap.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlMap.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL app IDs: " + missing);
        if (!extra.isEmpty()) {
            System.out.println("  EXTRA Mongo app IDs:");
            for (Long eid : extra) System.out.printf("    id=%d appNo=%s%n", eid, mongoMap.get(eid));
        }
    }

    // ── FINANCE DETAILS ───────────────────────────────────────────────────────
    private void compareFinanceDetails(Connection conn) throws SQLException {
        printSectionHeader("FINANCE DETAILS");
        Map<Long, Long> sqlMap = new LinkedHashMap<>(); // id -> application_id
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, application_id, loan_amount FROM finance_details ORDER BY id")) {
            while (rs.next()) {
                sqlMap.put(rs.getLong("id"), rs.getLong("application_id"));
                System.out.printf("  SQL Finance id=%-3d appId=%-3d loanAmt=%s%n",
                        rs.getLong("id"), rs.getLong("application_id"), rs.getString("loan_amount"));
            }
        }
        Map<Long, Object> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("finance_details")) {
            try {
                Long id = Long.parseLong(d.get("_id").toString());
                Object appRef = d.get("application");
                mongoMap.put(id, appRef);
                System.out.printf("  Mongo Finance id=%-3d appRef=%s loanAmt=%s%n", id, appRef, d.get("loanAmount"));
            } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlMap.keySet().stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Long> extra = mongoMap.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlMap.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL finance IDs: " + missing);
        if (!extra.isEmpty()) System.out.println("  EXTRA Mongo finance IDs (candidates for removal): " + extra);
    }

    // ── EMI PAYMENTS ──────────────────────────────────────────────────────────
    private void compareEmiPayments(Connection conn) throws SQLException {
        printSectionHeader("EMI PAYMENTS");
        Map<Long, String> sqlMap = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, finance_detail_id, installment_number, amount_paid FROM emi_payments ORDER BY id")) {
            while (rs.next()) {
                sqlMap.put(rs.getLong("id"), rs.getString("finance_detail_id") + "#" + rs.getInt("installment_number") + "#" + rs.getString("amount_paid"));
                System.out.printf("  SQL EMI id=%-3d fdId=%-3d emi#=%d amt=%s%n",
                        rs.getLong("id"), rs.getLong("finance_detail_id"),
                        rs.getInt("installment_number"), rs.getString("amount_paid"));
            }
        }
        Map<Long, Object> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("emi_payments")) {
            try {
                Long id = Long.parseLong(d.get("_id").toString());
                mongoMap.put(id, d.get("financeDetail"));
                System.out.printf("  Mongo EMI id=%-3d fdRef=%s emi#=%s amt=%s%n",
                        id, d.get("financeDetail"), d.get("installmentNumber"), d.get("amountPaid"));
            } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlMap.keySet().stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Long> extra = mongoMap.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlMap.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL EMI IDs: " + missing);
        if (!extra.isEmpty()) System.out.println("  EXTRA Mongo EMI IDs (candidates for removal): " + extra);
    }

    // ── BIKE INVENTORY ─────────────────────────────────────────────────────────
    private void compareBikeInventory(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM bike_inventory ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_inventory")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("BIKE_INVENTORY", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── BIKE INVENTORY IMAGES ────────────────────────────────────────────────
    private void compareBikeInventoryImages(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM bike_inventory_images ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_inventory_images")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("BIKE_INVENTORY_IMAGES", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── BIKE DETAILS ──────────────────────────────────────────────────────────
    private void compareBikeDetails(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM bike_details ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_details")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("BIKE_DETAILS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── DOCUMENTS ────────────────────────────────────────────────────────────
    private void compareDocuments(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM documents ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("documents")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("DOCUMENTS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────
    private void compareNotifications(Connection conn) throws SQLException {
        printSectionHeader("NOTIFICATIONS");
        Map<Long, String> sqlMap = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, user_id, title, type FROM notifications ORDER BY id")) {
            while (rs.next()) {
                sqlMap.put(rs.getLong("id"), rs.getString("title"));
                System.out.printf("  SQL Notif id=%-3d userId=%-3d type=%-20s title=%s%n",
                        rs.getLong("id"), rs.getLong("user_id"), rs.getString("type"), rs.getString("title"));
            }
        }
        Map<Long, String> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("notifications")) {
            try {
                Long id = Long.parseLong(d.get("_id").toString());
                mongoMap.put(id, d.getString("title"));
                System.out.printf("  Mongo Notif id=%-3d type=%-20s title=%s%n", id, d.getString("type"), d.getString("title"));
            } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlMap.keySet().stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Long> extra = mongoMap.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlMap.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING SQL notification IDs: " + missing);
        if (!extra.isEmpty()) {
            System.out.println("  EXTRA Mongo notification IDs:");
            for (Long eid : extra) System.out.printf("    id=%d title=%s%n", eid, mongoMap.get(eid));
        }
    }

    // ── EMI OVERDUE ALERTS ────────────────────────────────────────────────────
    private void compareEmiOverdueAlerts(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM emi_overdue_alerts ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("emi_overdue_alerts")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("EMI_OVERDUE_ALERTS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── BIKE OFFERS ──────────────────────────────────────────────────────────
    private void compareBikeOffers(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM bike_offers ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("bike_offers")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("BIKE_OFFERS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── WORKER TASKS ──────────────────────────────────────────────────────────
    private void compareWorkerTasks(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM worker_tasks ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("worker_tasks")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("WORKER_TASKS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── SERVICE JOBS ──────────────────────────────────────────────────────────
    private void compareServiceJobs(Connection conn) throws SQLException {
        Set<Long> sqlIds = new LinkedHashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM service_jobs ORDER BY id")) {
            while (rs.next()) sqlIds.add(rs.getLong("id"));
        }
        Set<Long> mongoIds = new LinkedHashSet<>();
        for (Document d : mongoAll("service_jobs")) {
            try { mongoIds.add(Long.parseLong(d.get("_id").toString())); } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlIds.stream().filter(id -> !mongoIds.contains(id)).collect(Collectors.toList());
        List<Long> extra = mongoIds.stream().filter(id -> !sqlIds.contains(id)).collect(Collectors.toList());
        printRow("SERVICE_JOBS", sqlIds.size(), mongoIds.size(), missing, extra);
    }

    // ── APPLICATION STATUS HISTORY ───────────────────────────────────────────
    private void compareAppStatusHistory(Connection conn) throws SQLException {
        printSectionHeader("APPLICATION STATUS HISTORY");
        Map<Long, String> sqlMap = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, application_id, new_status FROM application_status_history ORDER BY id")) {
            while (rs.next()) {
                sqlMap.put(rs.getLong("id"), rs.getLong("application_id") + "#" + rs.getString("new_status"));
                System.out.printf("  SQL History id=%-3d appId=%-3d newStatus=%s%n",
                        rs.getLong("id"), rs.getLong("application_id"), rs.getString("new_status"));
            }
        }
        Map<Long, String> mongoMap = new LinkedHashMap<>();
        for (Document d : mongoAll("application_status_history")) {
            try {
                Long id = Long.parseLong(d.get("_id").toString());
                mongoMap.put(id, d.getString("newStatus"));
                System.out.printf("  Mongo History id=%-3d newStatus=%s appRef=%s%n", id, d.getString("newStatus"), d.get("application"));
            } catch (Exception e) {/* skip */}
        }
        List<Long> missing = sqlMap.keySet().stream().filter(id -> !mongoMap.containsKey(id)).collect(Collectors.toList());
        List<Long> extra = mongoMap.keySet().stream().filter(id -> !sqlMap.containsKey(id)).collect(Collectors.toList());
        System.out.printf("%nSummary: SQL=%d | Mongo=%d | Missing=%d | Extra=%d%n",
                sqlMap.size(), mongoMap.size(), missing.size(), extra.size());
        if (!missing.isEmpty()) System.out.println("  MISSING: " + missing);
        if (!extra.isEmpty()) {
            System.out.println("  EXTRA Mongo History IDs:");
            for (Long eid : extra) System.out.printf("    id=%d status=%s%n", eid, mongoMap.get(eid));
        }
    }

    // ── ITEMS & SALES ─────────────────────────────────────────────────────────
    private void checkItemsAndSales() {
        printSectionHeader("LEGACY COLLECTIONS: items & sales");
        long itemCount = mongoTemplate.getCollection("items").countDocuments();
        long saleCount = mongoTemplate.getCollection("sales").countDocuments();
        System.out.println("  items collection documents: " + itemCount);
        System.out.println("  sales collection documents: " + saleCount);
        Document firstItem = mongoTemplate.getCollection("items").find().first();
        Document firstSale = mongoTemplate.getCollection("sales").find().first();
        if (firstItem != null) System.out.println("  Sample item _class: " + firstItem.getString("_class"));
        if (firstSale != null) System.out.println("  Sample sale _class: " + firstSale.getString("_class"));
        System.out.println("  STATUS: LEGACY collections from com.shopmanager — NOT used by com.autoconsultancy application code.");
        System.out.println("  ACTION: Awaiting explicit user confirmation before any deletion.");
    }
}
