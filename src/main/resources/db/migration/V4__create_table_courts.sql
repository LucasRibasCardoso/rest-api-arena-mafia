CREATE TABLE tb_courts
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    offset_minutes INTEGER      NOT NULL CHECK (offset_minutes IN (0, 30)),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_courts_name ON tb_courts (name);
CREATE INDEX idx_courts_is_active ON tb_courts (is_active);
CREATE INDEX idx_courts_offset_minutes ON tb_courts (offset_minutes);

