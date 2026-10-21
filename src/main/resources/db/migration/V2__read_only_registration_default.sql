-- Public self-registration must not grant write access by default.
-- Existing accounts are intentionally unchanged and must be reviewed separately before production use.
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'Read-Only';
