ALTER TABLE IF EXISTS post
    ADD post_view_counter BIGINT;

CREATE INDEX IF NOT EXISTS idx_post_published_at_filter
    ON post (deleted, published, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_comment_created_at
    ON comment (created_at DESC);