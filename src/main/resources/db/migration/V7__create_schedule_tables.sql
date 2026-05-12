CREATE TABLE tb_schedule_entries
(
    id         UUID        NOT NULL,
    court_id   UUID        NOT NULL,
    date       DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    entry_type VARCHAR(50) NOT NULL, -- Discriminador para identificar o tipo
    PRIMARY KEY (id),
    CONSTRAINT fk_schedule_entries_court FOREIGN KEY (court_id) REFERENCES tb_courts (id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_entries_court_id ON tb_schedule_entries (court_id);
CREATE INDEX idx_schedule_entries_date ON tb_schedule_entries (date);
CREATE INDEX idx_schedule_entries_court_date ON tb_schedule_entries (court_id, date);
CREATE INDEX idx_schedule_entries_type ON tb_schedule_entries (entry_type);

CREATE TABLE tb_reservations
(
    id                       UUID           NOT NULL,
    user_id                  UUID           NOT NULL,
    modality_id              UUID           NOT NULL,
    scheduled_by_admin_id    UUID,
    price                    DECIMAL(10, 2) NOT NULL,
    status                   VARCHAR(20)    NOT NULL,
    recurring_reservation_id UUID,
    cancelled_by_admin_id    UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservations_schedule_entry FOREIGN KEY (id) REFERENCES tb_schedule_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_modality FOREIGN KEY (modality_id) REFERENCES tb_modalities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_admin FOREIGN KEY (scheduled_by_admin_id) REFERENCES tb_users (id) ON DELETE SET NULL,
    CONSTRAINT chk_reservation_price CHECK (price >= 0)
);

CREATE INDEX idx_reservations_user_id ON tb_reservations (user_id);
CREATE INDEX idx_reservations_modality_id ON tb_reservations (modality_id);
CREATE INDEX idx_reservations_status ON tb_reservations (status);
CREATE INDEX idx_reservations_scheduled_by_admin ON tb_reservations (scheduled_by_admin_id);
CREATE INDEX idx_reservations_recurring ON tb_reservations (recurring_reservation_id);


CREATE TABLE tb_blocked_times
(
    id                        UUID         NOT NULL,
    description               VARCHAR(500) NOT NULL,
    blocked_by_admin_id       UUID         NOT NULL,
    is_full_day               BOOLEAN      NOT NULL,
    recurring_blocked_time_id UUID,
    PRIMARY KEY (id),
    CONSTRAINT fk_blocked_times_schedule_entry FOREIGN KEY (id) REFERENCES tb_schedule_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_times_admin FOREIGN KEY (blocked_by_admin_id) REFERENCES tb_users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_blocked_times_admin ON tb_blocked_times (blocked_by_admin_id);
CREATE INDEX idx_blocked_times_recurring ON tb_blocked_times (recurring_blocked_time_id);

