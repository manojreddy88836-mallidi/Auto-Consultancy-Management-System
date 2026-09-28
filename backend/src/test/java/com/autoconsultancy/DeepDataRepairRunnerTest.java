package com.autoconsultancy;

import com.autoconsultancy.entity.Manufacturer;
import com.autoconsultancy.repository.ManufacturerRepository;
import com.autoconsultancy.service.SequenceGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.Document;
import com.mongodb.DBRef;

import java.sql.*;
import java.util.*;

@SpringBootTest
public class DeepDataRepairRunnerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    private Connection getMysqlConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, "root", "root");
    }

    @Test
    public void executeDeepDataRepair() throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING SAFE DEEP DATA REPAIR & RECONCILIATION");
        System.out.println("==================================================");

        try (Connection conn = getMysqlConnection()) {
            repairManufacturers(conn);
            repairBikeModelsDbRefsAndNulls(conn);
            repairCustomerDbRefs();
            repairApplicationCustomerRefs();
            syncAllSequences();
        }

        System.out.println("==================================================");
        System.out.println("DEEP DATA REPAIR COMPLETED SUCCESSFULLY");
        System.out.println("==================================================");
    }

    private void repairManufacturers(Connection conn) throws SQLException {
        System.out.println("\n[REPAIR 1] Ensuring All 74 Manufacturers Exist in MongoDB");
        int added = 0;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM manufacturers")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String logoUrl = rs.getMetaData().getColumnCount() >= 3 ? rs.getString("logo_url") : null;
                String country = rs.getMetaData().getColumnCount() >= 4 ? rs.getString("country") : null;
                boolean active = rs.getBoolean("active");

                Manufacturer existing = mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), Manufacturer.class);
                if (existing == null) {
                    existing = manufacturerRepository.findById(id).orElse(null);
                }

                if (existing == null) {
                    Manufacturer m = Manufacturer.builder()
                            .id(id)
                            .name(name)
                            .logoUrl(logoUrl)
                            .country(country)
                            .active(active)
                            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                            .build();
                    manufacturerRepository.save(m);
                    added++;
                }
            }
        }
        System.out.printf("Manufacturers Repaired: Added=%d | Total in Mongo=%d\n", added, manufacturerRepository.count());
    }

    private void repairBikeModelsDbRefsAndNulls(Connection conn) throws SQLException {
        System.out.println("\n[REPAIR 2] Fixing Bike Models DBRefs & Null Manufacturers");
        int repairedCount = 0;

        // Map MySQL mfg ID -> mfg Name
        Map<Long, String> sqlMfgNames = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM manufacturers")) {
            while (rs.next()) {
                sqlMfgNames.put(rs.getLong("id"), rs.getString("name"));
            }
        }

        // Map Mongo mfg Name -> Mongo Manufacturer Long ID
        Map<String, Long> mongoMfgNameToId = new HashMap<>();
        for (Manufacturer m : manufacturerRepository.findAll()) {
            mongoMfgNameToId.put(m.getName().toLowerCase().trim(), m.getId());
        }

        // Iterate MySQL bike models and update Mongo documents with valid DBRef ($id as Long)
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, manufacturer_id, model_name FROM bike_models")) {
            while (rs.next()) {
                long modelId = rs.getLong("id");
                long sqlMfgId = rs.getLong("manufacturer_id");
                String mfgName = sqlMfgNames.get(sqlMfgId);

                if (mfgName != null) {
                    Long mongoMfgId = mongoMfgNameToId.get(mfgName.toLowerCase().trim());
                    if (mongoMfgId != null) {
                        DBRef dbRef = new DBRef("manufacturers", mongoMfgId);
                        mongoTemplate.getCollection("bike_models").updateOne(
                                new Document("_id", modelId),
                                new Document("$set", new Document("manufacturer", dbRef))
                        );
                        repairedCount++;
                    }
                }
            }
        }
        System.out.println("Bike Models Repaired with Valid DBRefs: " + repairedCount);
    }

    private void repairCustomerDbRefs() {
        System.out.println("\n[REPAIR 3] Converting Customer userRef DBRefs to Long $id");
        List<Document> custs = new ArrayList<>();
        mongoTemplate.getCollection("customers").find().into(custs);
        int repaired = 0;

        for (Document c : custs) {
            Object id = c.get("_id");
            Object userRefObj = c.get("user");
            if (userRefObj instanceof Document userDoc) {
                Object sId = userDoc.get("$id");
                if (sId != null) {
                    Long lId = Long.parseLong(sId.toString());
                    DBRef newRef = new DBRef("users", lId);
                    mongoTemplate.getCollection("customers").updateOne(
                            new Document("_id", id),
                            new Document("$set", new Document("user", newRef))
                    );
                    repaired++;
                }
            }
        }
        System.out.println("Customer userRefs Converted: " + repaired);
    }

    private void repairApplicationCustomerRefs() {
        System.out.println("\n[REPAIR 4] Fixing Application custRef & financeDetailRef DBRefs");
        List<Document> apps = new ArrayList<>();
        mongoTemplate.getCollection("applications").find().into(apps);
        int repaired = 0;

        for (Document app : apps) {
            Object id = app.get("_id");
            Object appNo = app.get("applicationNumber");

            // Fix custRef
            Object custRefObj = app.get("customer");
            if (custRefObj instanceof Document custDoc) {
                Object sId = custDoc.get("$id");
                if (sId != null) {
                    Long lId = Long.parseLong(sId.toString());
                    DBRef newRef = new DBRef("customers", lId);
                    mongoTemplate.getCollection("applications").updateOne(
                            new Document("_id", id),
                            new Document("$set", new Document("customer", newRef))
                    );
                    repaired++;
                }
            }

            // Fix AUTO-2026-00001 (App ID 19) missing customer ref
            if ("AUTO-2026-00001".equals(appNo)) {
                DBRef newRef = new DBRef("customers", 12L); // testcustomer@auto.com
                mongoTemplate.getCollection("applications").updateOne(
                        new Document("_id", id),
                        new Document("$set", new Document("customer", newRef))
                );
                System.out.println("Linked App AUTO-2026-00001 to Customer ID 12");
            }

            // Fix financeDetailRef
            Object fdRefObj = app.get("financeDetail");
            if (fdRefObj instanceof Document fdDoc) {
                Object sId = fdDoc.get("$id");
                if (sId != null) {
                    Long lId = Long.parseLong(sId.toString());
                    DBRef newRef = new DBRef("finance_details", lId);
                    mongoTemplate.getCollection("applications").updateOne(
                            new Document("_id", id),
                            new Document("$set", new Document("financeDetail", newRef))
                    );
                }
            }
        }
        System.out.println("Applications DBRefs Converted & Linked: " + repaired);
    }

    private void syncAllSequences() {
        System.out.println("\n[REPAIR 5] Synchronizing all database_sequences to MAX(id)");
        String[] collections = new String[]{
                "users", "customers", "workers", "manufacturers", "bike_models",
                "bike_variants", "manufacturing_years", "applications", "finance_details",
                "emi_payments", "documents", "notifications", "bike_inventory", "bike_inventory_images",
                "bike_details", "emi_overdue_alerts", "bike_offers", "service_jobs", "worker_tasks", "application_status_history"
        };
        for (String seqName : collections) {
            sequenceGenerator.resetSequenceToMax(seqName);
        }
        System.out.println("Synchronized all 20 collection sequence counters.");
    }
}
