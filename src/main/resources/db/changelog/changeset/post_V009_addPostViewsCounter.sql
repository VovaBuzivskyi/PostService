ALTER TABLE IF EXISTS post
    ADD IF NOT EXISTS post_view_counter BIGINT;