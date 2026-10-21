-- V1__init_schema.sql
-- IntelliShare Initial Relational Database Schema
-- Provides tables for users, cases, entities, evidence, timelines, criminal records, and audit logs.

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    badge_number VARCHAR(50) NOT NULL UNIQUE,
    agency VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'Officer' CHECK (role IN ('Administrator', 'Officer', 'Analyst', 'Read-Only')),
    department VARCHAR(100),
    phone VARCHAR(50),
    avatar_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_badge_number ON users(badge_number);
CREATE INDEX idx_users_agency ON users(agency);

-- 2. Cases table
CREATE TABLE IF NOT EXISTS cases (
    id BIGSERIAL PRIMARY KEY,
    case_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    case_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Registered' CHECK (status IN ('Registered', 'Under Investigation', 'Closed', 'Cold Case')),
    priority VARCHAR(20) NOT NULL DEFAULT 'Medium' CHECK (priority IN ('High', 'Medium', 'Low')),
    incident_date DATE,
    address VARCHAR(255),
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    agency VARCHAR(100) NOT NULL,
    assigned_officer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cases_case_number ON cases(case_number);
CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_case_type ON cases(case_type);
CREATE INDEX idx_cases_priority ON cases(priority);
CREATE INDEX idx_cases_city ON cases(city);
CREATE INDEX idx_cases_agency ON cases(agency);
CREATE INDEX idx_cases_assigned_officer ON cases(assigned_officer_id);

-- 3. Case Entities table (Person, Vehicle, Location, Phone, Organization)
CREATE TABLE IF NOT EXISTS case_entities (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('Person', 'Location', 'Vehicle', 'Phone', 'Organization')),
    entity_value VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_entities_case_id ON case_entities(case_id);
CREATE INDEX idx_case_entities_lookup ON case_entities(entity_type, entity_value);

-- 4. Case Evidence table (Digital evidence metadata, file hash, WORM / blockchain TX)
CREATE TABLE IF NOT EXISTS case_evidence (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_url VARCHAR(500),
    file_type VARCHAR(100),
    file_size_bytes BIGINT,
    sha256_hash VARCHAR(64),
    blockchain_tx_id VARCHAR(100),
    uploaded_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_evidence_case_id ON case_evidence(case_id);

-- 5. Case Timeline table
CREATE TABLE IF NOT EXISTS case_timelines (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    action VARCHAR(255) NOT NULL,
    details TEXT,
    performed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_timelines_case_id ON case_timelines(case_id);

-- 6. Criminal Records table (Registry, synthetic demo data flag)
CREATE TABLE IF NOT EXISTS criminal_records (
    id BIGSERIAL PRIMARY KEY,
    record_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    alias VARCHAR(150),
    date_of_birth DATE,
    age INT,
    gender VARCHAR(20) DEFAULT 'Male',
    status VARCHAR(50) NOT NULL DEFAULT 'Wanted' CHECK (status IN ('Wanted', 'In Custody', 'Surveillance', 'Released')),
    risk_level VARCHAR(20) NOT NULL DEFAULT 'Medium' CHECK (risk_level IN ('High', 'Medium', 'Low')),
    crime_type VARCHAR(100),
    last_seen_location VARCHAR(255),
    notes TEXT,
    photo_url VARCHAR(500),
    is_synthetic_demo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_criminal_records_num ON criminal_records(record_number);
CREATE INDEX idx_criminal_records_status ON criminal_records(status);
CREATE INDEX idx_criminal_records_risk ON criminal_records(risk_level);

-- 7. Audit Logs table (SHA-256 hash-chain immutable audit log)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    chain_index BIGINT NOT NULL UNIQUE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    actor_badge_number VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    details TEXT,
    previous_hash VARCHAR(64) NOT NULL,
    hash VARCHAR(64) NOT NULL
);

CREATE INDEX idx_audit_logs_chain_index ON audit_logs(chain_index);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
