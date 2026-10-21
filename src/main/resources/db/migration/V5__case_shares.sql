CREATE TABLE case_shares (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    owner_agency VARCHAR(100) NOT NULL,
    recipient_agency VARCHAR(100) NOT NULL,
    permission VARCHAR(10) NOT NULL CHECK (permission IN ('READ', 'EDIT')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    created_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_case_share_target UNIQUE(case_id, recipient_agency)
);

CREATE INDEX idx_case_shares_recipient_status ON case_shares(recipient_agency, status);
