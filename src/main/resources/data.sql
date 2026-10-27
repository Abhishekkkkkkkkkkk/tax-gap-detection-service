-- ==============================================================================
-- Seed Data: Prefilled Users and Mandatory Compliance Rules
-- ==============================================================================

-- Seed Users:
-- 'auditor' with password 'auditor123' (BCrypt hash)
-- 'admin' with password 'admin123' (BCrypt hash)
INSERT INTO users (username, password, role, enabled, created_at)
VALUES 
('auditor', '$2a$10$wE8F5R55t0P/d9N3qZ6w0.iXJ1zG1x.yD7.O6hVz1J/X3Z4c3jM8G', 'ROLE_AUDITOR', true, CURRENT_TIMESTAMP),
('admin', '$2a$10$wE8F5R55t0P/d9N3qZ6w0.iXJ1zG1x.yD7.O6hVz1J/X3Z4c3jM8G', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

-- Seed Mandatory Rules:
-- (A) High-Value Transaction Rule
INSERT INTO tax_rules (rule_name, description, enabled, severity, config_json, created_at)
VALUES (
    'HIGH_VALUE_TRANSACTION',
    'Flag transactions exceeding configured threshold (default 100,000.00)',
    true,
    'HIGH',
    '{"threshold": 100000.00}',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

-- (B) Refund Validation Rule
INSERT INTO tax_rules (rule_name, description, enabled, severity, config_json, created_at)
VALUES (
    'REFUND_VALIDATION',
    'Refund amount must not exceed original sale amount',
    true,
    'HIGH',
    '{"allowUnlinkedRefund": false}',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

-- (C) GST Slab Violation Rule
INSERT INTO tax_rules (rule_name, description, enabled, severity, config_json, created_at)
VALUES (
    'GST_SLAB_VIOLATION',
    'Flag transaction when amount exceeds slab threshold but taxRate is lower than required slab',
    true,
    'MEDIUM',
    '{"slabThreshold": 50000.00, "requiredTaxRate": 0.18}',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);
