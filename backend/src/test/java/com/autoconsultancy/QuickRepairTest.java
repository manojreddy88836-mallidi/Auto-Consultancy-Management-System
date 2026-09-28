package com.autoconsultancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Quick Repair Test:
 * 1. Fixes Application datetime fields stored as String → convert to BSON Date
 * 2. Fixes Customer user DBRef $id stored as String → convert to Long (proper DBRef)
 */
@SpringBootTest
public class QuickRepairTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Connection getMysql() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root");
    }

    @Test
    public void executeQuickRepair() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  QUICK REPAIR");
        System.out.println("=".repeat(60));

        fixApplicationDatetimes();
        fixCustomerUserRefs();
        fixAppStatusHistoryAppRefs();
        fixFinanceDetailAppRefs();
        fixEmiPaymentFinanceRefs();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  QUICK REPAIR COMPLETE");
        System.out.println("=".repeat(60));
    }

    // ─── 1. Fix Application datetime fields ───────────────────────────────────
    // The sync runner inserted createdAt/submittedAt as Strings "2026-09-03 20:27:47.492186"
    // Spring Data MongoDB expects BSON Date (java.util.Date) for LocalDateTime fields.
    // Use Timestamp.valueOf() which handles MySQL datetime format natively.
    private void fixApplicationDatetimes() {
        System.out.println("\n--- Fix Application datetime String → BSON Date ---");
        MongoCollection<Document> coll = mongoTemplate.getCollection("applications");

        List<Document> apps = new ArrayList<>();
        coll.find().into(apps);
        int fixed = 0;
        for (Document app : apps) {
            boolean changed = false;
            Document update = new Document();

            for (String field : Arrays.asList("createdAt", "updatedAt", "submittedAt")) {
                Object val = app.get(field);
                if (val instanceof String s && !s.isBlank()) {
                    try {
                        // Normalize: truncate sub-second precision for Timestamp.valueOf()
                        // Format: "2026-09-10 14:24:55.000000" → "2026-09-10 14:24:55.0"
                        String normalized = s.trim();
                        // Handle microseconds: Java Timestamp supports max nanoseconds
                        // Simplest: take only first 3 decimal places (milliseconds)
                        if (normalized.matches(".*\\.\\d+")) {
                            int dotIdx = normalized.lastIndexOf('.');
                            String intPart = normalized.substring(0, dotIdx);
                            String fracPart = normalized.substring(dotIdx + 1);
                            // Pad/trim to exactly 1 digit (Timestamp.valueOf needs .d at minimum)
                            fracPart = (fracPart + "000000000").substring(0, 9); // nanoseconds
                            normalized = intPart + "." + fracPart;
                        }
                        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(normalized);
                        update.put(field, new java.util.Date(ts.getTime()));
                        changed = true;
                    } catch (Exception e) {
                        // Try simple format without fractional seconds
                        try {
                            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(s.trim() + ".0");
                            update.put(field, new java.util.Date(ts.getTime()));
                            changed = true;
                        } catch (Exception e2) {
                            System.out.printf("  WARN: Could not parse date '%s' for field %s%n", s, field);
                        }
                    }
                }
            }

            if (changed) {
                Object appId = app.get("_id");
                coll.updateOne(Filters.eq("_id", appId), new Document("$set", update));
                System.out.printf("  Fixed app id=%s datetime fields%n", appId);
                fixed++;
            }
        }
        System.out.printf("  Applications datetime-fixed: %d%n", fixed);
    }


    // ─── 2. Fix Customer user DBRef $id String → Long ────────────────────────
    // The customer collection has: user: { "$ref": "users", "$id": "18" } (String)
    // Spring Data MongoDB @DBRef requires $id to be Long for proper resolution.
    private void fixCustomerUserRefs() {
        System.out.println("\n--- Fix Customer user DBRef $id String → Long ---");
        MongoCollection<Document> coll = mongoTemplate.getCollection("customers");
        List<Document> customers = new ArrayList<>();
        coll.find().into(customers);
        int fixed = 0;
        for (Document cust : customers) {
            Object rawId = cust.get("_id");
            Object userRef = cust.get("user");
            if (userRef instanceof Document userDoc) {
                Object refId = userDoc.get("$id");
                if (refId instanceof String s) {
                    try {
                        long longId = Long.parseLong(s);
                        Document newRef = new Document("$ref", "users").append("$id", longId);
                        coll.updateOne(Filters.eq("_id", rawId),
                            new Document("$set", new Document("user", newRef)));
                        System.out.printf("  Fixed customer id=%s userRef.$id '%s'→%d%n", rawId, s, longId);
                        fixed++;
                    } catch (NumberFormatException e) {
                        System.out.printf("  WARN: Could not parse userRef.$id '%s' for customer id=%s%n", refId, rawId);
                    }
                } else if (refId instanceof Long) {
                    System.out.printf("  OK customer id=%s userRef.$id already Long=%d%n", rawId, refId);
                }
            } else if (userRef instanceof com.mongodb.DBRef dbRef) {
                Object refId = dbRef.getId();
                if (refId instanceof String s) {
                    try {
                        long longId = Long.parseLong(s);
                        Document newRef = new Document("$ref", "users").append("$id", longId);
                        coll.updateOne(Filters.eq("_id", rawId),
                            new Document("$set", new Document("user", newRef)));
                        System.out.printf("  Fixed (DBRef) customer id=%s userRef.$id '%s'→%d%n", rawId, s, longId);
                        fixed++;
                    } catch (NumberFormatException e) {
                        System.out.printf("  WARN: Could not parse DBRef id '%s'%n", refId);
                    }
                } else {
                    System.out.printf("  OK customer id=%s DBRef.$id already %s=%s%n", rawId, refId.getClass().getSimpleName(), refId);
                }
            } else {
                System.out.printf("  customer id=%s userRef type=%s val=%s%n", rawId,
                    (userRef == null ? "null" : userRef.getClass().getSimpleName()), userRef);
            }
        }
        System.out.printf("  Customers user DBRef fixed: %d%n", fixed);
    }

    // ─── 3. Fix ApplicationStatusHistory appRef $id String → Long ────────────
    private void fixAppStatusHistoryAppRefs() {
        System.out.println("\n--- Fix ApplicationStatusHistory appRef $id String → Long ---");
        MongoCollection<Document> coll = mongoTemplate.getCollection("application_status_history");
        List<Document> docs = new ArrayList<>();
        coll.find().into(docs);
        int fixed = 0;
        for (Document d : docs) {
            Object rawId = d.get("_id");
            Object appRef = d.get("application");
            if (appRef instanceof Document appDoc) {
                Object refId = appDoc.get("$id");
                if (refId instanceof String s) {
                    try {
                        long longId = Long.parseLong(s);
                        Document newRef = new Document("$ref", "applications").append("$id", longId);
                        coll.updateOne(Filters.eq("_id", rawId),
                            new Document("$set", new Document("application", newRef)));
                        System.out.printf("  Fixed history id=%s appRef.$id '%s'→%d%n", rawId, s, longId);
                        fixed++;
                    } catch (NumberFormatException e) {
                        System.out.printf("  WARN: Could not parse appRef.$id '%s'%n", refId);
                    }
                }
            }
        }
        System.out.printf("  StatusHistory appRef fixed: %d%n", fixed);
    }

    // ─── 4. Fix FinanceDetail application appRef $id String/null → Long ─────────
    // SQL mapping: finance id → application id
    // 10→19, 11→20, 17→26, 18→27, 19→28, 20→29, 21→30
    private void fixFinanceDetailAppRefs() {
        System.out.println("\n--- Fix FinanceDetail appRef (null or String) → proper DBRef ---");
        MongoCollection<Document> coll = mongoTemplate.getCollection("finance_details");

        // Known SQL mapping from finance_details.id → applications.id
        Map<Long, Long> financeToApp = new LinkedHashMap<>();
        financeToApp.put(10L, 19L);
        financeToApp.put(11L, 20L);
        financeToApp.put(17L, 26L);
        financeToApp.put(18L, 27L);
        financeToApp.put(19L, 28L);
        financeToApp.put(20L, 29L);
        financeToApp.put(21L, 30L);

        List<Document> docs = new ArrayList<>();
        coll.find().into(docs);
        int fixed = 0;
        for (Document d : docs) {
            Object rawId = d.get("_id");
            long financeId;
            try { financeId = Long.parseLong(rawId.toString()); } catch (Exception e) { continue; }

            Long appId = financeToApp.get(financeId);
            if (appId == null) {
                System.out.printf("  SKIP: finance id=%s not in SQL mapping%n", rawId);
                continue;
            }

            Object appRef = d.get("application");
            boolean needsFix = false;

            if (appRef == null) {
                needsFix = true;
            } else if (appRef instanceof Document appDoc) {
                Object refId = appDoc.get("$id");
                if (refId instanceof String s) {
                    try {
                        long existing = Long.parseLong(s);
                        if (existing == appId) {
                            // Correct appId but stored as String → fix to Long
                            needsFix = true;
                        } else {
                            System.out.printf("  WARN: finance id=%s has appRef $id=%s but SQL says %d — keeping SQL value%n", rawId, s, appId);
                            needsFix = true;
                        }
                    } catch (NumberFormatException e) {
                        needsFix = true;
                    }
                } else if (refId instanceof Long existing) {
                    if (!existing.equals(appId)) {
                        System.out.printf("  WARN: finance id=%s appRef $id=%d but SQL says %d — fixing to SQL value%n", rawId, existing, appId);
                        needsFix = true;
                    } else {
                        System.out.printf("  OK: finance id=%s appRef already correct→app %d%n", rawId, appId);
                    }
                }
            }

            if (needsFix) {
                Document newRef = new Document("$ref", "applications").append("$id", appId);
                coll.updateOne(Filters.eq("_id", rawId), new Document("$set", new Document("application", newRef)));
                System.out.printf("  Fixed finance id=%s → appRef applications/%d%n", rawId, appId);
                fixed++;
            }
        }
        System.out.printf("  FinanceDetails appRef fixed: %d%n", fixed);
    }


    // ─── 5. Fix EmiPayment financeDetail ref $id String → Long ──────────────
    private void fixEmiPaymentFinanceRefs() {
        System.out.println("\n--- Fix EmiPayment financeDetailRef $id String → Long ---");
        MongoCollection<Document> coll = mongoTemplate.getCollection("emi_payments");
        List<Document> docs = new ArrayList<>();
        coll.find().into(docs);
        int fixed = 0;
        for (Document d : docs) {
            Object rawId = d.get("_id");
            Object fdRef = d.get("financeDetail");
            if (fdRef instanceof Document fdDoc) {
                Object refId = fdDoc.get("$id");
                if (refId instanceof String s) {
                    try {
                        long longId = Long.parseLong(s);
                        Document newRef = new Document("$ref", "finance_details").append("$id", longId);
                        coll.updateOne(Filters.eq("_id", rawId),
                            new Document("$set", new Document("financeDetail", newRef)));
                        fixed++;
                    } catch (NumberFormatException e) {
                        System.out.printf("  WARN: %s%n", refId);
                    }
                }
            }
        }
        System.out.printf("  EmiPayments financeDetailRef fixed: %d%n", fixed);
    }
}
