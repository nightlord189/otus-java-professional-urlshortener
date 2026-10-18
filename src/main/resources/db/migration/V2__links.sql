CREATE TABLE links
(
    id           BIGSERIAL PRIMARY KEY,
    short_code   VARCHAR(32)   NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    owner_id     BIGINT        NOT NULL REFERENCES users (id),
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_links_short_code UNIQUE (short_code),
    CONSTRAINT ck_links_status CHECK (status IN ('ACTIVE', 'DISABLED', 'BLOCKED', 'DELETED'))
);

CREATE INDEX ix_links_owner_created ON links (owner_id, created_at DESC);
