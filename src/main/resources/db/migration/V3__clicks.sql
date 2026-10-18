CREATE TABLE clicks
(
    id         BIGSERIAL PRIMARY KEY,
    link_id    BIGINT      NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    referer    VARCHAR(2048),
    user_agent VARCHAR(512),
    ip_hash    VARCHAR(64)
);

CREATE INDEX ix_clicks_link_clicked ON clicks (link_id, clicked_at);
