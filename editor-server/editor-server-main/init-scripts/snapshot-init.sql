-- init-scripts/snapshot-init.sql
-- Snapshot database initialization script

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Document snapshots table
CREATE TABLE IF NOT EXISTS document_snapshots (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL,
    fractional_position VARCHAR(500) NOT NULL,
    character VARCHAR(10),
    server_id VARCHAR(255) NOT NULL,
    server_seq_num BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Version vectors table
CREATE TABLE IF NOT EXISTS version_vectors (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL,
    server_id VARCHAR(255) NOT NULL,
    sequence_number BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(doc_id, server_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_doc_position ON document_snapshots(doc_id, fractional_position);
CREATE INDEX IF NOT EXISTS idx_doc_active ON document_snapshots(doc_id, active);
CREATE INDEX IF NOT EXISTS idx_version_doc ON version_vectors(doc_id);

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO snapshot_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO snapshot_user;
