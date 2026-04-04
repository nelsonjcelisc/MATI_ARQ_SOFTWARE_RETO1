-- =============================================================================
-- Seed Data for Admin API - CCP Security Platform
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Admin Profiles
-- -----------------------------------------------------------------------------
INSERT INTO admin_profile (admin_id, status, created_at, updated_at)
VALUES
    ('admin-001', 'ACTIVE', NOW(), NOW()),
    ('admin-002', 'ACTIVE', NOW(), NOW()),
    ('admin-003', 'LEARNING', NOW(), NOW()),
    ('admin-004', 'BLOCKED', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- Admin Trusted Contexts - Known devices, IPs, hours, and user agents
-- -----------------------------------------------------------------------------

-- Admin 001 - Trusted contexts
INSERT INTO admin_trusted_contexts (admin_profile_id, type, value, registered_at)
VALUES
    -- Devices
    (1, 'DEVICE', 'device-corporate-macbook-001', NOW()),
    (1, 'DEVICE', 'device-corporate-windows-001', NOW()),
    -- IPs
    (1, 'IP', '192.168.1.0/24', NOW()),
    (1, 'IP', '10.0.0.0/8', NOW()),
    -- Hours (business hours)
    (1, 'HOUR', '7', NOW()),
    (1, 'HOUR', '8', NOW()),
    (1, 'HOUR', '9', NOW()),
    (1, 'HOUR', '10', NOW()),
    (1, 'HOUR', '11', NOW()),
    (1, 'HOUR', '12', NOW()),
    (1, 'HOUR', '13', NOW()),
    (1, 'HOUR', '14', NOW()),
    (1, 'HOUR', '15', NOW()),
    (1, 'HOUR', '16', NOW()),
    (1, 'HOUR', '17', NOW()),
    (1, 'HOUR', '18', NOW()),
    -- User Agents
    (1, 'USER_AGENT', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', NOW()),
    (1, 'USER_AGENT', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', NOW()),
    -- Region
    (1, 'REGION', 'CO', NOW()),
    (1, 'REGION', 'US', NOW())
ON CONFLICT DO NOTHING;

-- Admin 002 - Trusted contexts
INSERT INTO admin_trusted_contexts (admin_profile_id, type, value, registered_at)
VALUES
    -- Devices
    (2, 'DEVICE', 'device-corporate-macbook-002', NOW()),
    -- IPs
    (2, 'IP', '192.168.2.0/24', NOW()),
    -- Hours (business hours)
    (2, 'HOUR', '8', NOW()),
    (2, 'HOUR', '9', NOW()),
    (2, 'HOUR', '10', NOW()),
    (2, 'HOUR', '11', NOW()),
    (2, 'HOUR', '12', NOW()),
    (2, 'HOUR', '13', NOW()),
    (2, 'HOUR', '14', NOW()),
    (2, 'HOUR', '15', NOW()),
    (2, 'HOUR', '16', NOW()),
    (2, 'HOUR', '17', NOW()),
    -- User Agents
    (2, 'USER_AGENT', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', NOW()),
    -- Region
    (2, 'REGION', 'CO', NOW())
ON CONFLICT DO NOTHING;

-- Admin 003 (LEARNING) - Minimal trusted contexts
INSERT INTO admin_trusted_contexts (admin_profile_id, type, value, registered_at)
VALUES
    (3, 'DEVICE', 'device-new-admin-003', NOW()),
    (3, 'IP', '192.168.3.0/24', NOW()),
    (3, 'REGION', 'CO', NOW())
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- Admin Context Events - Historical access log
-- -----------------------------------------------------------------------------
INSERT INTO admin_context_events (admin_id, device, ip, hour, agent, score, decision, timestamp)
VALUES
    -- Admin 001 - Normal access patterns
    ('admin-001', 'device-corporate-macbook-001', '192.168.1.100', 9, 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 0.1, 'ALLOWED', NOW() - INTERVAL '2 days'),
    ('admin-001', 'device-corporate-macbook-001', '192.168.1.100', 14, 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 0.15, 'ALLOWED', NOW() - INTERVAL '1 day'),
    ('admin-001', 'device-corporate-windows-001', '10.0.0.50', 10, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 0.2, 'ALLOWED', NOW() - INTERVAL '12 hours'),

    -- Admin 001 - Blocked anomalous access attempt
    ('admin-001', 'unknown-device-xyz', '45.33.32.156', 3, 'python-requests/2.28.0', 0.95, 'BLOCKED', NOW() - INTERVAL '6 hours'),

    -- Admin 002 - Normal access
    ('admin-002', 'device-corporate-macbook-002', '192.168.2.50', 11, 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 0.12, 'ALLOWED', NOW() - INTERVAL '1 day'),
    ('admin-002', 'device-corporate-macbook-002', '192.168.2.50', 15, 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 0.1, 'ALLOWED', NOW() - INTERVAL '5 hours')
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- Vendor Commissions
-- -----------------------------------------------------------------------------
INSERT INTO vendor_commission (vendor_id, commission_rate, effective_from, effective_to, active, created_at, updated_at, created_by)
VALUES
    ('vendor-001', 5.50, NOW() - INTERVAL '30 days', NULL, true, NOW(), NOW(), 'admin-001'),
    ('vendor-002', 7.25, NOW() - INTERVAL '60 days', NULL, true, NOW(), NOW(), 'admin-001'),
    ('vendor-003', 4.00, NOW() - INTERVAL '90 days', NOW() - INTERVAL '30 days', false, NOW(), NOW(), 'admin-002'),
    ('vendor-003', 5.00, NOW() - INTERVAL '30 days', NULL, true, NOW(), NOW(), 'admin-002'),
    ('vendor-004', 6.00, NOW() - INTERVAL '15 days', NULL, true, NOW(), NOW(), 'admin-001')
ON CONFLICT DO NOTHING;
