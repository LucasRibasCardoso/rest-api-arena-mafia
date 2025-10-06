CREATE TABLE tb_modalities
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_modalities_name ON tb_modalities (name);
