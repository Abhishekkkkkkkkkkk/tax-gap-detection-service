-- ==============================================================================
-- Schema Definition: Tax Gap Detection & Compliance Validation Service
-- ==============================================================================

-- 1. Users Table (for Authentication & Role-Based Access Control)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tax Rules Table (Configurable Rule Engine)
CREATE TABLE IF NOT EXISTS tax_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    severity VARCHAR(20) NOT NULL,
    config_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. Transactions Table (Ingested Raw & Computed Records)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    transaction_date DATE,
    customer_id VARCHAR(100),
    amount DECIMAL(18, 2),
    tax_rate DECIMAL(8, 4),
    reported_tax DECIMAL(18, 2),
    transaction_type VARCHAR(20),
    original_transaction_id VARCHAR(100),
    validation_status VARCHAR(20) NOT NULL,
    failure_reasons TEXT,
    raw_payload TEXT,
    expected_tax DECIMAL(18, 2),
    tax_gap DECIMAL(18, 2),
    compliance_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tx_customer (customer_id),
    INDEX idx_tx_compliance (compliance_status),
    INDEX idx_tx_date (transaction_date)
);

-- 4. Exceptions Table
CREATE TABLE IF NOT EXISTS exceptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100),
    rule_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ex_tx (transaction_id),
    INDEX idx_ex_cust (customer_id),
    INDEX idx_ex_rule (rule_name),
    INDEX idx_ex_severity (severity)
);

-- 5. Audit Logs Table (Immutable Audit Trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL,
    transaction_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detail_json TEXT,
    INDEX idx_audit_event (event_type),
    INDEX idx_audit_tx (transaction_id),
    INDEX idx_audit_time (timestamp)
);
