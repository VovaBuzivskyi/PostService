CREATE TABLE hashtag (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    hashtag varchar(4096) NOT NULL,
    post_id bigint NOT NULL
);

CREATE INDEX idx_hashtag ON hashtag (hashtag);