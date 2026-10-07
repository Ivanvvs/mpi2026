-- A vote stores only the selected option.  Participation is tracked separately
-- by voting_receipts, which intentionally has no reference to a vote.
ALTER TABLE votes ADD COLUMN option_id BIGINT;

-- Legacy Base64 values and deterministic voter hashes must not be retained:
-- together they would continue to reveal historical voter-to-choice mappings.
DELETE FROM votes;

ALTER TABLE votes ALTER COLUMN option_id SET NOT NULL;
ALTER TABLE votes ADD CONSTRAINT fk_votes_option
    FOREIGN KEY (option_id) REFERENCES voting_options (id);
ALTER TABLE votes DROP CONSTRAINT IF EXISTS uk_vote_anonymous_voter;
ALTER TABLE votes DROP COLUMN encrypted_value;
ALTER TABLE votes DROP COLUMN anonymous_voter_hash;

CREATE INDEX IF NOT EXISTS idx_votes_option_id ON votes (option_id);
