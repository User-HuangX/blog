CREATE TABLE IF NOT EXISTS post_meta (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_content (
    post_id BIGINT PRIMARY KEY,
    summary VARCHAR(500),
    content TEXT,
    CONSTRAINT fk_post_content_meta
        FOREIGN KEY (post_id)
        REFERENCES post_meta (id)
        ON DELETE CASCADE
);
