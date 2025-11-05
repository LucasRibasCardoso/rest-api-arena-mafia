CREATE TABLE tb_modalities
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL UNIQUE,
    is_active  BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_modalities_name ON tb_modalities (name);
