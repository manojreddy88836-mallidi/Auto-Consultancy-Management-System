package com.autoconsultancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.sql.*;
import java.util.*;

@SpringBootTest
public class DataComparisonTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Connection getMysqlConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, "root", "root");
    }

    @Test
    public void compareDatabases() throws Exception {
        System.out.println("==================================================");
        System.out.println("DATABASE COMPARISON REPORT: MYSQL VS MONGODB");
        System.out.println("==================================================");

        Set<String> mongoCollections = mongoTemplate.getCollectionNames();

        List<String> mysqlTables = new ArrayList<>();
        Map<String, Long> mysqlCounts = new LinkedHashMap<>();
        Map<String, Long> mongoCounts = new LinkedHashMap<>();

        try (Connection conn = getMysqlConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables("auto_consultancy", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    mysqlTables.add(rs.getString("TABLE_NAME"));
                }
            }

            for (String table : mysqlTables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                    if (rs.next()) {
                        mysqlCounts.put(table, rs.getLong(1));
                    }
                }
            }
        }

        for (String coll : mongoCollections) {
            long count = mongoTemplate.getCollection(coll).countDocuments();
            mongoCounts.put(coll, count);
        }

        System.out.printf("%-30s %-15s %-15s\n", "TABLE / COLLECTION", "MYSQL COUNT", "MONGODB COUNT");
        System.out.println("------------------------------------------------------------------");

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(mysqlCounts.keySet());
        allNames.addAll(mongoCounts.keySet());

        for (String name : allNames) {
            Long myCount = mysqlCounts.getOrDefault(name, 0L);
            Long moCount = mongoCounts.getOrDefault(name, 0L);
            System.out.printf("%-30s %-15d %-15d\n", name, myCount, moCount);
        }

        System.out.println("==================================================");
    }
}
