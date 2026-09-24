ALTER TABLE short_links ADD COLUMN expires_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE short_links ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE short_links ADD COLUMN click_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE short_links ADD COLUMN last_clicked_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE short_links ADD CONSTRAINT ck_short_links_click_count CHECK (click_count >= 0);
