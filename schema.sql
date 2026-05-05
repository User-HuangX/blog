CREATE TABLE IF NOT EXISTS post_meta
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    author_name  VARCHAR(100) NOT NULL,
    is_published BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE post_meta
    ADD COLUMN IF NOT EXISTS is_published BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_post_meta_is_published_created_at
    ON post_meta (is_published, created_at DESC);

CREATE TABLE IF NOT EXISTS post_content
(
    post_id BIGINT PRIMARY KEY,
    summary VARCHAR(500),
    content TEXT,
    CONSTRAINT fk_post_content_meta
        FOREIGN KEY (post_id)
            REFERENCES post_meta (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_image
(
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
