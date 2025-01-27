ALTER TABLE post
    ADD post_view_counter BIGINT DEFAULT 0;

CREATE INDEX idx_post_published_at
    ON post (deleted, published, published_at DESC);

