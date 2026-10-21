CREATE TABLE ingestion_jobs (
    id UUID PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    rows_processed INTEGER NOT NULL DEFAULT 0,
    agency VARCHAR(100) NOT NULL,
    created_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ingestion_jobs_agency_created ON ingestion_jobs(agency, created_at DESC);
