ALTER TABLE privilege_requests
    ADD COLUMN IF NOT EXISTS required_s_points INTEGER NOT NULL DEFAULT 0;

ALTER TABLE votes
    DROP COLUMN IF EXISTS session_id;
