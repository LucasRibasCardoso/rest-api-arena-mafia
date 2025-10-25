CREATE TABLE tb_operating_hours
(
    id          UUID                     NOT NULL PRIMARY KEY,
    day_of_week VARCHAR(10)              NOT NULL,
    open_time   TIME                     NOT NULL,
    close_time  TIME                     NOT NULL,
    is_active   BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_operating_hours_day_of_week
        CHECK (day_of_week IN
               ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))

);

CREATE INDEX idx_operating_hours_day_of_week ON tb_operating_hours (day_of_week);
CREATE INDEX idx_operating_hours_is_active ON tb_operating_hours (is_active);
CREATE INDEX idx_operating_hours_open_time ON tb_operating_hours (open_time);
CREATE INDEX idx_operating_hours_close_time ON tb_operating_hours (close_time);
