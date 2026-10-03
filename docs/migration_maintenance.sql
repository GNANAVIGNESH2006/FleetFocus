-- ============================================================================
-- FleetFocus Maintenance Schema Migration (AG-13)
-- IMPORTANT: DO NOT execute this migration automatically in production.
-- Review and run manually against the target MySQL database.
-- ============================================================================

-- 1. Add nullable lifecycle columns
ALTER TABLE maintenance_logs
    ADD COLUMN status VARCHAR(32) NULL,
    ADD COLUMN completed_date DATE NULL;

-- 2. Backfill existing maintenance records as COMPLETED
UPDATE maintenance_logs
SET status = 'COMPLETED',
    completed_date = service_date
WHERE status IS NULL;

-- 3. Enforce NOT NULL constraint on status
ALTER TABLE maintenance_logs
    MODIFY COLUMN status VARCHAR(32) NOT NULL;

-- 4. Convert cost column to exact decimal precision DECIMAL(12,2)
ALTER TABLE maintenance_logs
    MODIFY COLUMN cost DECIMAL(12,2) NOT NULL;

-- 5. Expand description column length to VARCHAR(500)
ALTER TABLE maintenance_logs
    MODIFY COLUMN description VARCHAR(500) NOT NULL;
