CREATE TABLE tb_courts
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(100)             NOT NULL,
    description    TEXT,
    offset_minutes INTEGER                  NOT NULL CHECK (offset_minutes IN (0, 30)),
    is_active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_courts_name ON tb_courts (name);
CREATE INDEX idx_courts_is_active ON tb_courts (is_active);
CREATE INDEX idx_courts_offset_minutes ON tb_courts (offset_minutes);

CREATE TABLE tb_court_modalities
(
    court_id    UUID NOT NULL,
    modality_id UUID NOT NULL,
    PRIMARY KEY (court_id, modality_id),
    CONSTRAINT fk_court_modalities_court FOREIGN KEY (court_id)
        REFERENCES tb_courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_court_modalities_modality FOREIGN KEY (modality_id)
        REFERENCES tb_modalities (id) ON DELETE CASCADE
);

CREATE INDEX idx_court_modalities_court_id ON tb_court_modalities (court_id);
CREATE INDEX idx_court_modalities_modality_id ON tb_court_modalities (modality_id);


