package com.autoconsultancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class DeepReconciliationInspectionTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void inspectBikeModelsManufacturerDbRefs() {
        System.out.println("==================================================");
        System.out.println("INSPECTING BIKE MODELS MANUFACTURER DBREFS");
        System.out.println("==================================================");

        List<Document> rawModels = new ArrayList<>();
        mongoTemplate.getCollection("bike_models").find().into(rawModels);

        int nullMfgCount = 0;
        int stringIdCount = 0;
        int longIdCount = 0;
        int missingMfgDocCount = 0;

        for (Document m : rawModels) {
            Object id = m.get("_id");
            Object mfgRef = m.get("manufacturer");

            if (mfgRef == null) {
                nullMfgCount++;
                System.out.println("BikeModel _id=" + id + " | modelName=" + m.get("modelName") + " | manufacturer is NULL!");
            } else if (mfgRef instanceof Document mfgDoc) {
                Object refId = mfgDoc.get("$id");
                if (refId instanceof String sId) {
                    stringIdCount++;
                    System.out.println("BikeModel _id=" + id + " | manufacturer DBRef $id is STRING: '" + sId + "'");
                } else if (refId instanceof Long lId) {
                    longIdCount++;
                    Document foundMfg = mongoTemplate.getCollection("manufacturers").find(new Document("_id", lId)).first();
                    if (foundMfg == null) {
                        missingMfgDocCount++;
                        System.out.println("BikeModel _id=" + id + " | manufacturer DBRef points to missing Manufacturer ID=" + lId);
                    }
                }
            } else {
                System.out.println("BikeModel _id=" + id + " | manufacturer type=" + mfgRef.getClass().getName() + " -> " + mfgRef);
            }
        }

        System.out.println("\n--- SUMMARY FOR BIKE MODELS ---");
        System.out.println("Total Bike Models: " + rawModels.size());
        System.out.println("Null Manufacturer Refs: " + nullMfgCount);
        System.out.println("String $id DBRefs: " + stringIdCount);
        System.out.println("Long $id DBRefs: " + longIdCount);
        System.out.println("Missing Manufacturer Targets: " + missingMfgDocCount);
    }
}
