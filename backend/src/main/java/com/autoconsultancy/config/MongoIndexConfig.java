package com.autoconsultancy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import org.bson.Document;

/**
 * MongoIndexConfig — creates MongoDB indexes on startup.
 *
 * These indexes are idempotent (ensureIndex). They are critical for:
 * - Applications list page (status filter, createdAt sort, search)
 * - Dashboard count queries (status-based counts)
 * - Notification queries (userId + read status)
 * - Worker task queries (worker.id + status/type)
 * - Bike inventory queries (active + saleStatus + modelId)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        try {
            createApplicationIndexes();
            createNotificationIndexes();
            createWorkerTaskIndexes();
            createBikeInventoryIndexes();
            createCustomerIndexes();
            createBikeInventoryImageIndexes();
            log.info("MongoDB indexes ensured successfully.");
        } catch (Exception e) {
            log.warn("Failed to ensure MongoDB indexes (non-fatal): {}", e.getMessage());
        }
    }

    private void createApplicationIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("applications");

        // Used by: countByStatus, findByStatus pageable queries
        ops.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_applications_status"));

        // Used by: getAll() sort by createdAt, admin dashboard month count
        ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_applications_createdAt"));

        // Used by: getAll() with status filter + createdAt sort
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("status", 1).append("createdAt", -1))
                .named("idx_applications_status_createdAt"));

        // Used by: findByCustomerId, countByCustomerId
        ops.ensureIndex(new Index().on("customer.id", Sort.Direction.ASC).named("idx_applications_customerId"));

        // Used by: findByWorkerUserId, countActiveAssignmentsByWorkerUserId
        ops.ensureIndex(new Index().on("workerAssignment.worker.user.id", Sort.Direction.ASC)
                .named("idx_applications_workerUserId"));

        // Used by: applicationNumber uniqueness + search — index already exists via @Indexed on entity
        // Note: Do NOT recreate with different options — MongoDB would throw IndexOptionsConflict
        try {
            ops.ensureIndex(new Index().on("applicationNumber", Sort.Direction.ASC)
                    .named("idx_applications_applicationNumber"));
        } catch (Exception ex) {
            log.debug("applicationNumber index already exists with different settings, skipping: {}", ex.getMessage());
        }

        log.debug("Application indexes ensured.");
    }

    private void createNotificationIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("notifications");

        // Used by: unread count poll (Topbar), fetchNotifications
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("user.id", 1).append("read", 1))
                .named("idx_notifications_userId_read"));

        // Used by: getNotifications() sort
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("user.id", 1).append("createdAt", -1))
                .named("idx_notifications_userId_createdAt"));

        log.debug("Notification indexes ensured.");
    }

    private void createWorkerTaskIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("worker_tasks");

        // Used by: findByWorkerId, countByWorkerIdAndStatus
        ops.ensureIndex(new Index().on("worker.id", Sort.Direction.ASC).named("idx_workertasks_workerId"));

        // Used by: findByWorkerIdAndStatus
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("worker.id", 1).append("status", 1))
                .named("idx_workertasks_workerId_status"));

        // Used by: findByWorkerIdAndType
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("worker.id", 1).append("taskType", 1))
                .named("idx_workertasks_workerId_type"));

        // Used by: sort, admin listing
        ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_workertasks_createdAt"));

        log.debug("WorkerTask indexes ensured.");
    }

    private void createBikeInventoryIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("bike_inventory");

        // Used by: findByActiveTrue (most queries start with active=true)
        ops.ensureIndex(new Index().on("active", Sort.Direction.ASC).named("idx_bikeinventory_active"));

        // Used by: findByActiveTrueAndSaleStatus
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("active", 1).append("saleStatus", 1))
                .named("idx_bikeinventory_active_saleStatus"));

        // Used by: findByActiveTrueAndModelId
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("active", 1).append("bikeModel.id", 1))
                .named("idx_bikeinventory_active_modelId"));

        // Used by: findByActiveTrueAndManufacturerId
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("active", 1).append("bikeModel.manufacturer.id", 1))
                .named("idx_bikeinventory_active_manufacturerId"));

        // Used by: countBySaleStatus
        ops.ensureIndex(new Index().on("saleStatus", Sort.Direction.ASC).named("idx_bikeinventory_saleStatus"));

        log.debug("BikeInventory indexes ensured.");
    }

    private void createCustomerIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("customers");

        // Used by: countNewThisMonth (createdAt range query)
        ops.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_customers_createdAt"));

        log.debug("Customer indexes ensured.");
    }

    private void createBikeInventoryImageIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("bike_inventory_images");

        // Used by: buildInventoryImageUrl — find primary image by inventoryId
        ops.ensureIndex(new CompoundIndexDefinition(
                new Document("bikeInventory.id", 1).append("primary", -1))
                .named("idx_bikeimages_inventoryId_primary"));

        log.debug("BikeInventoryImage indexes ensured.");
    }
}
