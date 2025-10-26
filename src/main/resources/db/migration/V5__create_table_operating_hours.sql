CREATE TABLE tb_operating_hours
(
    id         UUID                     NOT NULL PRIMARY KEY,
    start_time  TIME                     NOT NULL,
    end_time TIME                     NOT NULL,
    is_active  BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_operating_hours_is_active ON tb_operating_hours (is_active);
CREATE INDEX idx_operating_hours_start_time ON tb_operating_hours (start_time);
CREATE INDEX idx_operating_hours_end_time ON tb_operating_hours (end_time);

CREATE TABLE tb_operating_hours_days
(
    operating_hours_id UUID        NOT NULL,
    day_of_week        VARCHAR(20) NOT NULL,
    PRIMARY KEY (operating_hours_id, day_of_week),
    CONSTRAINT fk_operating_hours_days_to_hours FOREIGN KEY (operating_hours_id) REFERENCES tb_operating_hours (id) ON DELETE CASCADE
);

CREATE INDEX idx_operating_hours_days_operating_hours_id ON tb_operating_hours_days (operating_hours_id);
