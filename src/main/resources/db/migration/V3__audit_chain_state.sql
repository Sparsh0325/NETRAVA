-- Serialize audit appends and scope the audit feed to an agency.
ALTER TABLE audit_logs ADD COLUMN agency VARCHAR(100);

CREATE TABLE audit_chain_state (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    last_index BIGINT NOT NULL,
    last_hash VARCHAR(64) NOT NULL
);

INSERT INTO audit_chain_state (id, last_index, last_hash)
SELECT 1, COALESCE(MAX(chain_index), 0),
       COALESCE((SELECT hash FROM audit_logs ORDER BY chain_index DESC LIMIT 1), repeat('0', 64))
FROM audit_logs;
