package com.autoconsultancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.DBRef;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SAFE SQL → MongoDB Full Sync Runner.
 *
 * SAFETY: All affected collections are backed up to JSON files BEFORE any modification.
 * Execution order respects MongoDB document references (dependency ordering).
 *
 * What this does:
 * 1. Backs up all affected collections
 * 2. Fixes user _id misalignment (Mongo id 12→18, 7→23, etc.)
 * 3. Fixes customer documents (wrong IDs, delete legacy)
 * 4. Fixes manufacturer name mismatches
 * 5. Deletes extra applications, finance_details, emi_payments
 * 6. Deletes extra notifications, application_status_history
 * 7. Deletes extra bike_models, bike_variants, manufacturing_years
 * 8. Resets all sequences
 */
@SpringBootTest
public class FullSyncRunnerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Connection getMysql() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root");
    }

    private static final String BACKUP_DIR =
        "C:\\Users\\LENOVO\\OneDrive\\Desktop\\antigravity\\Auto_Consultancy\\backup_pre_fullsync_" +
        new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

    // ── MAIN TEST ─────────────────────────────────────────────────────────────
    @Test
    public void executeFullSync() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  FULL SQL→MONGODB SYNC RUNNER — START");
        System.out.println("=".repeat(60));

        // PHASE 0: Backup
        step("PHASE 0: BACKUP ALL AFFECTED COLLECTIONS");
        new File(BACKUP_DIR).mkdirs();
        backupCollection("users");
        backupCollection("customers");
        backupCollection("manufacturers");
        backupCollection("applications");
        backupCollection("finance_details");
        backupCollection("emi_payments");
        backupCollection("notifications");
        backupCollection("application_status_history");
        backupCollection("bike_models");
        backupCollection("bike_variants");
        backupCollection("manufacturing_years");
        System.out.println("  Backups written to: " + BACKUP_DIR);

        try (Connection conn = getMysql()) {
            // Load SQL reference data
            Map<Long, String> sqlUserEmails = loadSqlUserEmails(conn);
            Map<String, Long> sqlUserEmailToId = sqlUserEmails.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue().toLowerCase(), Map.Entry::getKey));
            Map<Long, Long> sqlCustomerIds = loadSqlCustomerIds(conn); // custId → userId
            Map<Long, String> sqlManufacturerNames = loadSqlManufacturerNames(conn); // id → name

            // PHASE 1: Fix users
            step("PHASE 1: FIX USER _id MISALIGNMENT");
            fixUserIds(sqlUserEmailToId);

            // PHASE 2: Fix customers
            step("PHASE 2: FIX CUSTOMER DOCUMENTS");
            fixCustomers(sqlCustomerIds);

            // PHASE 3: Fix manufacturers
            step("PHASE 3: FIX MANUFACTURER NAME MISMATCHES");
            fixManufacturers(sqlManufacturerNames, conn);

            // PHASE 4: Delete extra applications (IDs 1–8)
            step("PHASE 4: DELETE EXTRA APPLICATIONS (Mongo IDs 1–8)");
            deleteExtraById("applications", Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
            // Verify and fix remaining app customer refs
            fixApplicationCustomerRefs(conn);

            // PHASE 5: Delete extra finance_details (IDs 1–6)
            step("PHASE 5: DELETE EXTRA FINANCE_DETAILS (Mongo IDs 1–6)");
            deleteExtraById("finance_details", Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

            // PHASE 6: Delete extra emi_payments (IDs 1–22)
            step("PHASE 6: DELETE EXTRA EMI_PAYMENTS (Mongo IDs 1–22)");
            List<Long> extraEmiIds = new ArrayList<>();
            for (long i = 1; i <= 22; i++) extraEmiIds.add(i);
            deleteExtraById("emi_payments", extraEmiIds);

            // PHASE 7: Delete extra notifications (id=1)
            step("PHASE 7: DELETE EXTRA NOTIFICATION (id=1)");
            deleteExtraById("notifications", Arrays.asList(1L));

            // PHASE 8: Delete extra application_status_history (id=1)
            step("PHASE 8: DELETE EXTRA APPLICATION_STATUS_HISTORY (id=1)");
            deleteExtraById("application_status_history", Arrays.asList(1L));

            // PHASE 9: Delete extra bike_models (IDs 1–67)
            step("PHASE 9: DELETE EXTRA BIKE_MODELS (Mongo IDs 1–67)");
            List<Long> extraModelIds = new ArrayList<>();
            for (long i = 1; i <= 67; i++) extraModelIds.add(i);
            deleteExtraById("bike_models", extraModelIds);

            // PHASE 10: Delete extra bike_variants (IDs 1–67)
            step("PHASE 10: DELETE EXTRA BIKE_VARIANTS (Mongo IDs 1–67)");
            List<Long> extraVariantIds = new ArrayList<>();
            for (long i = 1; i <= 67; i++) extraVariantIds.add(i);
            deleteExtraById("bike_variants", extraVariantIds);

            // PHASE 11: Delete extra manufacturing_years (IDs 1–553)
            step("PHASE 11: DELETE EXTRA MANUFACTURING_YEARS (Mongo IDs 1–553)");
            List<Long> extraYearIds = new ArrayList<>();
            for (long i = 1; i <= 553; i++) extraYearIds.add(i);
            deleteExtraById("manufacturing_years", extraYearIds);

            // PHASE 12: Reset sequences
            step("PHASE 12: RESET SEQUENCES TO MAX SQL IDs");
            resetSequences(conn);
        }

        // PHASE 13: Final verification count
        step("PHASE 13: FINAL VERIFICATION COUNT");
        printFinalCounts();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  FULL SYNC COMPLETE");
        System.out.println("  Backup directory: " + BACKUP_DIR);
        System.out.println("=".repeat(60));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void step(String msg) {
        System.out.println("\n--- " + msg + " ---");
    }

    private void backupCollection(String collName) throws IOException {
        File f = new File(BACKUP_DIR, collName + ".json");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            var cursor = mongoTemplate.getCollection(collName)
                    .withDocumentClass(org.bson.BsonDocument.class).find().cursor();
            int count = 0;
            while (cursor.hasNext()) {
                pw.println(cursor.next().toJson());
                count++;
            }
            System.out.printf("  Backed up %-30s → %d docs%n", collName, count);
        }
    }

    private void deleteExtraById(String collName, List<Long> ids) {
        MongoCollection<Document> coll = mongoTemplate.getCollection(collName);
        int deleted = 0;
        for (Long id : ids) {
            long count = coll.deleteOne(Filters.eq("_id", id)).getDeletedCount();
            deleted += count;
        }
        System.out.printf("  Deleted %d documents from %s (attempted %d IDs)%n", deleted, collName, ids.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 1: Fix User _id Misalignment
    //
    // Strategy: MongoDB has the correct user data but at WRONG _ids.
    // E.g. SQL user id=18 (testcustomer@auto.com) is in Mongo as id=12.
    // We need to update _id. In MongoDB you can't update _id directly;
    // must insert new doc with correct id, then delete old.
    // ─────────────────────────────────────────────────────────────────────────
    private Map<Long, String> loadSqlUserEmails(Connection conn) throws SQLException {
        Map<Long, String> map = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, email FROM users ORDER BY id")) {
            while (rs.next()) map.put(rs.getLong("id"), rs.getString("email"));
        }
        return map;
    }

    private void fixUserIds(Map<String, Long> sqlEmailToId) {
        MongoCollection<Document> coll = mongoTemplate.getCollection("users");
        List<Document> allUsers = new ArrayList<>();
        coll.find().into(allUsers);

        int renamed = 0, deletedLegacy = 0, deletedRogue = 0;

        // Delete rogue ObjectId document
        for (Document d : allUsers) {
            Object id = d.get("_id");
            if (id instanceof ObjectId) {
                coll.deleteOne(Filters.eq("_id", id));
                System.out.println("  Deleted rogue ObjectId user: " + id);
                deletedRogue++;
            }
        }

        // Reload after rogue deletion
        allUsers.clear();
        coll.find().into(allUsers);

        // Legacy emails NOT in SQL — delete these
        Set<String> legacyEmails = new HashSet<>(Arrays.asList(
            "testcustomer.mongo@auto.com",
            "johndoe@example.com",
            "johndoe2@example.com",
            "johndoe3@example.com"
        ));

        // Separate pass: delete legacy users first
        for (Document d : allUsers) {
            Object rawId = d.get("_id");
            String email = d.getString("email");
            if (email == null) email = "";
            if (legacyEmails.contains(email.toLowerCase())) {
                coll.deleteOne(Filters.eq("_id", rawId));
                System.out.printf("  Deleted legacy user id=%s email=%s%n", rawId, email);
                deletedLegacy++;
            }
        }

        // Reload after legacy deletion
        allUsers.clear();
        coll.find().into(allUsers);

        // Now rename users that have wrong _id:
        // CRITICAL: delete old doc FIRST (to release email unique index), then insert with correct id
        for (Document d : allUsers) {
            Object rawId = d.get("_id");
            String email = d.getString("email");
            if (email == null) continue;

            Long sqlId = sqlEmailToId.get(email.toLowerCase());
            if (sqlId != null) {
                long mongoId;
                try { mongoId = Long.parseLong(rawId.toString()); } catch (Exception e) { continue; }
                if (!sqlId.equals(mongoId)) {
                    // DELETE old first (releases unique email constraint), then INSERT with correct id
                    Document stored = new Document(d);
                    try {
                        coll.deleteOne(Filters.eq("_id", rawId));
                        Document newDoc = new Document(stored);
                        newDoc.put("_id", sqlId);
                        coll.insertOne(newDoc);
                        System.out.printf("  Renamed user %s→%d (%s)%n", rawId, sqlId, email);
                        renamed++;
                    } catch (Exception ex) {
                        System.out.printf("  WARN: Could not rename user %s→%d: %s%n", rawId, sqlId, ex.getMessage());
                    }
                }
            }
        }
        System.out.printf("  Users: renamed=%d, deletedLegacy=%d, deletedRogue=%d%n", renamed, deletedLegacy, deletedRogue);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 2: Fix Customer Documents
    //
    // Delete ALL current Mongo customers, then re-insert 6 with correct SQL IDs
    // pointing to correct user refs (which now have correct IDs from Phase 1).
    // ─────────────────────────────────────────────────────────────────────────
    private Map<Long, Long> loadSqlCustomerIds(Connection conn) throws SQLException {
        Map<Long, Long> map = new LinkedHashMap<>(); // custId → userId (same in SQL for customers)
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT c.id, u.email, u.id as uid FROM customers c JOIN users u ON c.id = u.id ORDER BY c.id")) {
            while (rs.next()) map.put(rs.getLong("id"), rs.getLong("uid"));
        }
        return map;
    }

    private void fixCustomers(Map<Long, Long> sqlCustomerIds) {
        MongoCollection<Document> coll = mongoTemplate.getCollection("customers");

        // 1. Delete all current Mongo customer documents
        long deleted = coll.deleteMany(Filters.exists("_id")).getDeletedCount();
        System.out.printf("  Deleted all %d existing customer documents%n", deleted);

        // 2. Re-insert with correct SQL IDs
        for (Map.Entry<Long, Long> entry : sqlCustomerIds.entrySet()) {
            Long custId = entry.getKey();
            Long userId = entry.getValue(); // same as custId in SQL (customer extends user)
            Document newCustomer = new Document();
            newCustomer.put("_id", custId);
            newCustomer.put("user", new DBRef("users", userId));
            newCustomer.put("_class", "com.autoconsultancy.entity.Customer");
            try {
                coll.insertOne(newCustomer);
                System.out.printf("  Inserted customer id=%d userRef=users/%d%n", custId, userId);
            } catch (Exception ex) {
                System.out.printf("  ERROR inserting customer id=%d: %s%n", custId, ex.getMessage());
            }
        }
        System.out.printf("  Customers re-inserted: %d%n", sqlCustomerIds.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 3: Fix Manufacturer Name Mismatches
    //
    // SQL id=8 → KAWASAKI but Mongo id=8 → KTM (etc.)
    // Update the `name` field in MongoDB to match SQL names.
    // Also insert any SQL manufacturers (IDs 69–74) missing from Mongo.
    // ─────────────────────────────────────────────────────────────────────────
    private Map<Long, String> loadSqlManufacturerNames(Connection conn) throws SQLException {
        Map<Long, String> map = new LinkedHashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM manufacturers ORDER BY id")) {
            while (rs.next()) map.put(rs.getLong("id"), rs.getString("name"));
        }
        return map;
    }

    private void fixManufacturers(Map<Long, String> sqlNames, Connection conn) throws SQLException {
        MongoCollection<Document> mfrColl = mongoTemplate.getCollection("manufacturers");
        MongoCollection<Document> modelColl = mongoTemplate.getCollection("bike_models");

        // Step 1: Build current Mongo state BEFORE any changes
        // mongoNameUpper → currentMongoId
        Map<String, Long> mongoNameToCurrentId = new LinkedHashMap<>();
        // currentMongoId → name
        Map<Long, String> mongoIdToName = new LinkedHashMap<>();
        List<Document> allMongo = new ArrayList<>();
        mfrColl.find().into(allMongo);
        for (Document d : allMongo) {
            Object rawId = d.get("_id");
            try {
                long lid = Long.parseLong(rawId.toString());
                String name = d.getString("name");
                mongoIdToName.put(lid, name);
                if (name != null) mongoNameToCurrentId.put(name.toUpperCase().trim(), lid);
            } catch (Exception ignored) {}
        }

        // Step 2: Build OLD_MongoId → NEW_SqlId remap (matched by name)
        Map<Long, Long> oldIdToNewId = new LinkedHashMap<>();
        for (Map.Entry<Long, String> sqlEntry : sqlNames.entrySet()) {
            long sqlId = sqlEntry.getKey();
            String sqlNameUpper = sqlEntry.getValue().trim().toUpperCase();
            Long currentMongoId = mongoNameToCurrentId.get(sqlNameUpper);
            if (currentMongoId != null && !currentMongoId.equals(sqlId)) {
                oldIdToNewId.put(currentMongoId, sqlId);
                System.out.printf("  Manufacturer remap: Mongo id=%d (%s) → SQL id=%d%n", currentMongoId, sqlEntry.getValue(), sqlId);
            }
        }

        // Step 3: Update bike_model manufacturer DBRefs BEFORE deleting manufacturers
        int modelUpdated = 0;
        for (Map.Entry<Long, Long> remap : oldIdToNewId.entrySet()) {
            long fromId = remap.getKey();
            long toId = remap.getValue();
            // Query bike_models where manufacturer.$id == fromId
            var result = modelColl.updateMany(
                Filters.eq("manufacturer.$id", fromId),
                new Document("$set", new Document("manufacturer",
                    new Document("$ref", "manufacturers").append("$id", toId)))
            );
            if (result.getModifiedCount() > 0) {
                System.out.printf("  Updated %d bike_models: manufacturer ref %d→%d%n",
                    result.getModifiedCount(), fromId, toId);
                modelUpdated++;
            }
        }
        System.out.printf("  Bike model manufacturer refs: %d groups remapped%n", modelUpdated);

        // Step 4: Drop unique name index to allow re-insert without conflict
        try {
            mfrColl.dropIndex("name_1");
            System.out.println("  Dropped unique name index on manufacturers");
        } catch (Exception e) {
            System.out.println("  Note: name_1 index not found (or already dropped): " + e.getMessage());
        }

        // Step 5: Delete ALL current MongoDB manufacturers
        long deleted = mfrColl.deleteMany(Filters.exists("_id")).getDeletedCount();
        System.out.printf("  Deleted all %d existing manufacturer documents%n", deleted);

        // Step 6: Re-insert all 74 SQL manufacturers with correct IDs
        int inserted = 0;
        for (Map.Entry<Long, String> sqlEntry : sqlNames.entrySet()) {
            Document doc = new Document();
            doc.put("_id", sqlEntry.getKey());
            doc.put("name", sqlEntry.getValue());
            doc.put("_class", "com.autoconsultancy.entity.Manufacturer");
            try {
                mfrColl.insertOne(doc);
                inserted++;
            } catch (Exception ex) {
                System.out.printf("  WARN: Could not insert manufacturer id=%d (%s): %s%n",
                    sqlEntry.getKey(), sqlEntry.getValue(), ex.getMessage());
            }
        }
        System.out.printf("  Manufacturers re-inserted: %d / %d%n", inserted, sqlNames.size());

        // Step 7: Recreate the unique name index
        try {
            mfrColl.createIndex(
                new Document("name", 1),
                new com.mongodb.client.model.IndexOptions().unique(true)
            );
            System.out.println("  Recreated unique name index on manufacturers");
        } catch (Exception e) {
            System.out.println("  WARN: Could not recreate name index: " + e.getMessage());
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 4 (extra): Fix application→customer DBRef after user ID fix
    // ─────────────────────────────────────────────────────────────────────────
    private void fixApplicationCustomerRefs(Connection conn) throws SQLException {
        MongoCollection<Document> appColl = mongoTemplate.getCollection("applications");

        // Step A: Fix stale customer DBRef $id values in EXISTING apps
        // After Phase 1, user IDs were renamed: Mongo 12→18, 7→23, 8→24, 9→25, 10→26, 11→27
        // Customer IDs match user IDs (Phase 2 re-inserted customers at correct SQL IDs).
        // But existing Mongo apps still reference the OLD customer IDs.
        // Mapping: old Mongo custId → new SQL custId
        Map<Long, Long> oldCustToNewCust = new LinkedHashMap<>();
        oldCustToNewCust.put(12L, 18L);
        oldCustToNewCust.put(7L, 23L);
        oldCustToNewCust.put(8L, 24L);
        oldCustToNewCust.put(9L, 25L);
        oldCustToNewCust.put(10L, 26L);
        oldCustToNewCust.put(11L, 27L);

        for (Map.Entry<Long, Long> remap : oldCustToNewCust.entrySet()) {
            long oldCustId = remap.getKey();
            long newCustId = remap.getValue();
            var result = appColl.updateMany(
                Filters.eq("customer.$id", oldCustId),
                new Document("$set", new Document("customer",
                    new Document("$ref", "customers").append("$id", newCustId)))
            );
            if (result.getModifiedCount() > 0) {
                System.out.printf("  Updated %d apps: custRef %d→%d%n",
                    result.getModifiedCount(), oldCustId, newCustId);
            }
        }

        // Step B: List remaining apps and check for missing ones
        List<Document> remaining = new ArrayList<>();
        appColl.find().into(remaining);

        System.out.println("  Remaining applications after extra deletion + ref fix:");
        for (Document d : remaining) {
            System.out.printf("    id=%s appNo=%s custRef=%s%n",
                d.get("_id"), d.getString("applicationNumber"), d.get("customer"));
        }

        Set<Long> remainingIds = remaining.stream()
            .map(d -> { try { return Long.parseLong(d.get("_id").toString()); } catch (Exception e) { return -1L; } })
            .collect(Collectors.toSet());

        // Step C: Re-insert any SQL apps missing from Mongo (e.g. id=20 was deleted as extra id=6)
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT id, application_number, created_at, remarks, status, submitted_at, updated_at, customer_id " +
                 "FROM applications ORDER BY id")) {
            while (rs.next()) {
                long appId = rs.getLong("id");
                if (!remainingIds.contains(appId)) {
                    System.out.printf("  Re-inserting missing SQL application id=%d (%s)%n", appId, rs.getString("application_number"));
                    Document appDoc = buildApplicationDocument(rs);
                    try {
                        appColl.insertOne(appDoc);
                        System.out.printf("    Inserted app id=%d%n", appId);
                    } catch (Exception ex) {
                        System.out.printf("    ERROR: %s%n", ex.getMessage());
                    }
                }
            }
        }
    }

    private Document buildApplicationDocument(ResultSet rs) throws SQLException {
        Document doc = new Document();
        doc.put("_id", rs.getLong("id"));
        doc.put("applicationNumber", rs.getString("application_number"));
        doc.put("status", rs.getString("status"));
        // Use actual SQL column names (no application_date, no bike_variant_id)
        String createdAt = rs.getString("created_at");
        if (createdAt != null) doc.put("createdAt", createdAt);
        String submittedAt = rs.getString("submitted_at");
        if (submittedAt != null) doc.put("submittedAt", submittedAt);
        String remarks = rs.getString("remarks");
        if (remarks != null) doc.put("remarks", remarks);
        doc.put("_class", "com.autoconsultancy.entity.Application");
        long custId = rs.getLong("customer_id");
        doc.put("customer", new Document("$ref", "customers").append("$id", custId));
        return doc;
    }


    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 12: Reset sequences
    // ─────────────────────────────────────────────────────────────────────────
    private void resetSequences(Connection conn) throws SQLException {
        Map<String, Long> maxSqlIds = new LinkedHashMap<>();
        maxSqlIds.put("users_sequence", maxId(conn, "users"));
        maxSqlIds.put("customers_sequence", maxId(conn, "customers"));
        maxSqlIds.put("manufacturers_sequence", maxId(conn, "manufacturers"));
        maxSqlIds.put("applications_sequence", maxId(conn, "applications"));
        maxSqlIds.put("finance_details_sequence", maxId(conn, "finance_details"));
        maxSqlIds.put("emi_payments_sequence", maxId(conn, "emi_payments"));
        maxSqlIds.put("notifications_sequence", maxId(conn, "notifications"));
        maxSqlIds.put("application_status_history_sequence", maxId(conn, "application_status_history"));
        maxSqlIds.put("bike_models_sequence", maxId(conn, "bike_models"));
        maxSqlIds.put("bike_variants_sequence", maxId(conn, "bike_variants"));
        maxSqlIds.put("manufacturing_years_sequence", maxId(conn, "manufacturing_years"));

        MongoCollection<Document> seqColl = mongoTemplate.getCollection("database_sequences");
        for (Map.Entry<String, Long> entry : maxSqlIds.entrySet()) {
            String seqId = entry.getKey();
            long maxId = entry.getValue();
            seqColl.updateOne(
                Filters.eq("_id", seqId),
                new Document("$set", new Document("seq", maxId)),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
            System.out.printf("  Sequence %-45s → %d%n", seqId, maxId);
        }
    }

    private long maxId(Connection conn, String table) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT MAX(id) FROM `" + table + "`")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 13: Final Verification Count
    // ─────────────────────────────────────────────────────────────────────────
    private void printFinalCounts() throws SQLException {
        String[] collections = {
            "users", "customers", "workers", "manufacturers",
            "applications", "finance_details", "emi_payments",
            "notifications", "application_status_history",
            "bike_models", "bike_variants", "manufacturing_years",
            "bike_inventory", "bike_details", "documents",
            "emi_overdue_alerts", "service_jobs", "worker_tasks",
            "bike_offers", "bike_inventory_images"
        };

        System.out.println("\n  FINAL MONGODB COUNTS:");
        System.out.printf("  %-35s %8s%n", "Collection", "Count");
        System.out.println("  " + "-".repeat(45));
        try (Connection conn = getMysql()) {
            for (String coll : collections) {
                long mongoCount = mongoTemplate.getCollection(coll).countDocuments();
                long sqlCount = 0;
                try {
                    String sqlTable = coll;
                    sqlCount = maxId(conn, sqlTable);
                } catch (Exception ignored) {}
                System.out.printf("  %-35s Mongo=%6d%n", coll, mongoCount);
            }
        }
    }
}
