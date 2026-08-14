ALTER TABLE stored_files ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY';
ALTER TABLE stored_files ADD COLUMN delete_requested_at TIMESTAMPTZ;
ALTER TABLE stored_files ADD CONSTRAINT ck_stored_files_status CHECK (status IN ('READY', 'DELETING'));

CREATE INDEX idx_stored_files_deletion_retry
    ON stored_files(delete_requested_at)
    WHERE status = 'DELETING';
