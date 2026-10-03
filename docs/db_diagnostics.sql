-- ============================================================================
-- FleetFocus Database Diagnostics (AG-17)
-- IMPORTANT: DO NOT run repairs automatically.
-- This file contains ONLY read-only SELECT queries to identify data anomalies.
-- Suggested repair statements are provided strictly as commented-out SQL for
-- manual review by the database owner.
-- ============================================================================

-- 1. Vehicles with >1 ACTIVE trip
SELECT
    t.vehicle_id,
    v.license_plate,
    v.vin,
    COUNT(*) AS active_trip_count
FROM trips t
JOIN vehicles v ON v.id = t.vehicle_id
WHERE t.status = 'ACTIVE'
GROUP BY t.vehicle_id, v.license_plate, v.vin
HAVING COUNT(*) > 1;

-- Suggested manual repair (review trip IDs before running):
-- UPDATE trips SET status = 'COMPLETED', end_time = NOW(), distance_covered = COALESCE(distance_covered, 0)
-- WHERE id = <duplicate_active_trip_id> AND status = 'ACTIVE';


-- 2. Drivers with >1 ACTIVE trip
SELECT
    t.driver_id,
    d.name,
    d.license_number,
    COUNT(*) AS active_trip_count
FROM trips t
JOIN drivers d ON d.id = t.driver_id
WHERE t.status = 'ACTIVE'
GROUP BY t.driver_id, d.name, d.license_number
HAVING COUNT(*) > 1;

-- Suggested manual repair (review trip IDs before running):
-- UPDATE trips SET status = 'COMPLETED', end_time = NOW(), distance_covered = COALESCE(distance_covered, 0)
-- WHERE id = <duplicate_active_trip_id> AND status = 'ACTIVE';


-- 3. Drivers marked ON_TRIP with no ACTIVE trip
SELECT
    d.id,
    d.name,
    d.license_number,
    d.username,
    d.status
FROM drivers d
LEFT JOIN trips t ON t.driver_id = d.id AND t.status = 'ACTIVE'
WHERE d.status = 'ON_TRIP'
  AND t.id IS NULL;

-- Suggested manual repair:
-- UPDATE drivers d
-- LEFT JOIN trips t ON t.driver_id = d.id AND t.status = 'ACTIVE'
-- SET d.status = 'AVAILABLE'
-- WHERE d.status = 'ON_TRIP' AND t.id IS NULL;


-- 4. Vehicles marked ON_TRIP with no ACTIVE trip
SELECT
    v.id,
    v.license_plate,
    v.vin,
    v.model,
    v.status
FROM vehicles v
LEFT JOIN trips t ON t.vehicle_id = v.id AND t.status = 'ACTIVE'
WHERE v.status = 'ON_TRIP'
  AND t.id IS NULL;

-- Suggested manual repair:
-- UPDATE vehicles v
-- LEFT JOIN trips t ON t.vehicle_id = v.id AND t.status = 'ACTIVE'
-- SET v.status = 'AVAILABLE'
-- WHERE v.status = 'ON_TRIP' AND t.id IS NULL;


-- 5. Vehicles whose VIN length != 17 or violates standard VIN characters
SELECT
    v.id,
    v.vin,
    CHAR_LENGTH(v.vin) AS vin_length,
    v.license_plate,
    v.model
FROM vehicles v
WHERE CHAR_LENGTH(v.vin) <> 17
   OR v.vin NOT REGEXP '^[A-HJ-NPR-Z0-9]{17}$';

-- Suggested manual repair (replace with a valid 17-character VIN per vehicle):
-- UPDATE vehicles SET vin = '1FTFW1ET5DFC10312' WHERE id = <vehicle_id>;


-- 6. Driver usernames with no matching SystemUser (or linked to non-DRIVER user)
SELECT
    d.id AS driver_id,
    d.name AS driver_name,
    d.username AS driver_username,
    u.id AS user_id,
    u.role AS user_role
FROM drivers d
LEFT JOIN system_users u ON LOWER(u.username) = LOWER(d.username)
WHERE d.username IS NOT NULL
  AND d.username <> ''
  AND (u.id IS NULL OR u.role <> 'DRIVER');

-- Suggested manual repair:
-- UPDATE drivers d
-- LEFT JOIN system_users u ON LOWER(u.username) = LOWER(d.username)
-- SET d.username = NULL
-- WHERE d.username IS NOT NULL AND (u.id IS NULL OR u.role <> 'DRIVER');


-- 7. DRIVER-role SystemUsers without a linked Driver profile
SELECT
    u.id AS user_id,
    u.username,
    u.email,
    u.role
FROM system_users u
LEFT JOIN drivers d ON LOWER(d.username) = LOWER(u.username)
WHERE u.role = 'DRIVER'
  AND d.id IS NULL;

-- Suggested manual repair (link an existing unlinked driver profile):
-- UPDATE drivers SET username = '<driver_username>' WHERE id = <driver_id> AND username IS NULL;


-- 8. Maintenance logs with a future service_date
SELECT
    m.id,
    m.vehicle_id,
    v.license_plate,
    m.service_date,
    m.description,
    m.cost
FROM maintenance_logs m
LEFT JOIN vehicles v ON v.id = m.vehicle_id
WHERE m.service_date > CURRENT_DATE;

-- Suggested manual repair:
-- UPDATE maintenance_logs SET service_date = CURRENT_DATE WHERE id = <maintenance_id> AND service_date > CURRENT_DATE;
