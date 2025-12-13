-- init-scripts/primary-init.sql
-- Primary database initialization script

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create tables (Spring JPA will also create these, but this ensures consistency)
CREATE TABLE IF NOT EXISTS crdt_operations (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    server_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    character VARCHAR(10),
    fractional_position VARCHAR(500) NOT NULL,
    server_seq_num BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_doc_seq ON crdt_operations(doc_id, server_seq_num);
CREATE INDEX IF NOT EXISTS idx_doc_server_seq ON crdt_operations(doc_id, server_id, server_seq_num);
CREATE INDEX IF NOT EXISTS idx_timestamp ON crdt_operations(timestamp);
CREATE INDEX IF NOT EXISTS idx_processed ON crdt_operations(processed);

-- Configure for Debezium CDC
ALTER TABLE crdt_operations REPLICA IDENTITY FULL;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO admin;

---

