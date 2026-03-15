CREATE INDEX idx_rooms_deleted_created
    ON discussion_rooms (deleted_at, created_at DESC);