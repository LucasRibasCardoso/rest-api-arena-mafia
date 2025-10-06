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
