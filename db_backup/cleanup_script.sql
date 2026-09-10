-- ============================================================
-- AUTO CONSULTANCY — FULL TEST DATA CLEANUP SCRIPT
-- Created: 2026-08-18
-- Safe backup: db_backup/auto_consultancy_backup_20260818_100726.sql
-- ============================================================
-- WHAT IS BEING DELETED:
--   All transactional test/dev data:
--     - Test customer accounts (9 users)
--     - Test applications (5 records)
--     - Test bike inventory (14 test records, keeping NONE)
--     - Test bike offers (9 records)
--     - Test documents (8 records)
--     - Test finance details (2 records)
--     - Test notifications (13 records)
--     - Test status history (4 records)
--     - Test worker assignments (1 record)
--     - Test bike details (5 records)
--     - Test bike inventory images (15 records)
--
-- WHAT IS BEING PRESERVED:
--   - users 1-4 (admin + workers)
--   - All manufacturers (69), bike_models (555), bike_variants (555), manufacturing_years (3127)
--   - Database schema / table structure
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Clear notifications (all test)
DELETE FROM notifications;
SELECT ROW_COUNT() AS notifications_deleted;

-- 2. Clear application_status_history (all test)
DELETE FROM application_status_history;
SELECT ROW_COUNT() AS status_history_deleted;

-- 3. Clear worker_assignments (all test)
DELETE FROM worker_assignments;
SELECT ROW_COUNT() AS worker_assignments_deleted;

-- 4. Clear documents (all test)
DELETE FROM documents;
SELECT ROW_COUNT() AS documents_deleted;

-- 5. Clear finance_details (all test)
DELETE FROM finance_details;
SELECT ROW_COUNT() AS finance_details_deleted;

-- 6. Clear bike_details (all test)
DELETE FROM bike_details;
SELECT ROW_COUNT() AS bike_details_deleted;

-- 7. Clear bike_offers (all test)
DELETE FROM bike_offers;
SELECT ROW_COUNT() AS bike_offers_deleted;

-- 8. Clear applications (all test)
DELETE FROM applications;
SELECT ROW_COUNT() AS applications_deleted;

-- 9. Clear bike_inventory_images (all test)
DELETE FROM bike_inventory_images;
SELECT ROW_COUNT() AS bike_inv_images_deleted;

-- 10. Clear bike_inventory (all test physical bikes)
DELETE FROM bike_inventory;
SELECT ROW_COUNT() AS bike_inventory_deleted;

-- 11. Clear bike_images (old/legacy table — empty anyway)
DELETE FROM bike_images;
SELECT ROW_COUNT() AS bike_images_deleted;

-- 12. Clear audit_logs (empty anyway)
DELETE FROM audit_logs;
SELECT ROW_COUNT() AS audit_logs_deleted;

-- 13. Remove test/demo customer accounts (users 5-13)
--     Keep: 1 (admin), 2-4 (workers)
DELETE FROM customers WHERE id IN (5,6,7,8,9,10,11,12,13);
SELECT ROW_COUNT() AS customers_deleted;

DELETE FROM users WHERE id IN (5,6,7,8,9,10,11,12,13);
SELECT ROW_COUNT() AS users_deleted;

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;

-- 14. VERIFY remaining data
SELECT 'users remaining:' AS label, COUNT(*) AS count FROM users;
SELECT 'customers remaining:' AS label, COUNT(*) AS count FROM customers;
SELECT 'applications remaining:' AS label, COUNT(*) AS count FROM applications;
SELECT 'bike_inventory remaining:' AS label, COUNT(*) AS count FROM bike_inventory;
SELECT 'bike_offers remaining:' AS label, COUNT(*) AS count FROM bike_offers;
SELECT 'documents remaining:' AS label, COUNT(*) AS count FROM documents;
SELECT 'manufacturers remaining:' AS label, COUNT(*) AS count FROM manufacturers;
SELECT 'bike_models remaining:' AS label, COUNT(*) AS count FROM bike_models;
SELECT 'bike_variants remaining:' AS label, COUNT(*) AS count FROM bike_variants;
SELECT 'manufacturing_years remaining:' AS label, COUNT(*) AS count FROM manufacturing_years;
SELECT 'notifications remaining:' AS label, COUNT(*) AS count FROM notifications;

-- Show remaining users (should be admin + 3 workers only)
SELECT id, email, role, first_name, last_name FROM users ORDER BY id;
