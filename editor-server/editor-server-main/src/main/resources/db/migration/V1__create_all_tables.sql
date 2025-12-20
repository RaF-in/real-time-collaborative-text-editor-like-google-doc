-- ========================================
-- V1__create_all_tables.sql
-- ========================================
-- NOTE: Using snake_case (PostgreSQL standard)
-- JPA will automatically map camelCase fields to snake_case columns

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS crdt_operations (
                                               id BIGSERIAL PRIMARY KEY,
                                               doc_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    server_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    character VARCHAR(10),
    fractional_position VARCHAR(500) NOT NULL,
    server_seq_num BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                            processed BOOLEAN NOT NULL DEFAULT FALSE
                            );

CREATE INDEX IF NOT EXISTS idx_doc_seq
    ON crdt_operations (doc_id, server_seq_num);

CREATE INDEX IF NOT EXISTS idx_doc_server_seq
    ON crdt_operations (doc_id, server_id, server_seq_num);

CREATE TABLE IF NOT EXISTS crdt_operation_outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
