-- Create documents table in the snapshot database
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add document_id foreign key column to document_snapshots table
ALTER TABLE document_snapshots
ADD COLUMN document_id BIGINT NOT NULL REFERENCES documents(id);

-- Add document_id foreign key column to version_vectors table
ALTER TABLE version_vectors
ADD COLUMN document_id BIGINT NOT NULL REFERENCES documents(id);

-- Create indexes for better performance
CREATE INDEX idx_documents_owner_id ON documents(owner_id);
CREATE INDEX idx_documents_doc_id ON documents(doc_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_documents_updated_at ON documents(updated_at);
CREATE INDEX idx_documents_owner_updated ON documents(owner_id, updated_at DESC);

-- Create indexes for foreign keys
CREATE INDEX idx_document_snapshots_document_id ON document_snapshots(document_id);
CREATE INDEX idx_version_vectors_document_id ON version_vectors(document_id);

-- Add trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_documents_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_documents_updated_at();