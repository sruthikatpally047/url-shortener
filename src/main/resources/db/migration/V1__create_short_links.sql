CREATE TABLE short_links (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_short_links_code UNIQUE (code)
);
