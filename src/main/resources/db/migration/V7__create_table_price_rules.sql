CREATE TABLE tb_price_rules
(
    id         UUID                     NOT NULL PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    start_time TIME,
    end_time   TIME,
    price      DECIMAL(10, 2)           NOT NULL,
    priority   INTEGER                  NOT NULL,
    is_active  BOOLEAN                  NOT NULL DEFAULT TRUE,
    is_default BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_price_rules_time_range
        CHECK ((start_time IS NULL AND end_time IS NULL) OR
               (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)),
    CONSTRAINT chk_price_rules_price_positive
        CHECK (price >= 0),
    CONSTRAINT chk_price_rules_priority_positive
        CHECK (priority >= 0)
);


CREATE TABLE tb_price_rule_days
(
    price_rule_id UUID        NOT NULL,
    day_of_week   VARCHAR(20) NOT NULL,
    PRIMARY KEY (price_rule_id, day_of_week),
    CONSTRAINT fk_price_rule_days_to_rule FOREIGN KEY (price_rule_id) REFERENCES tb_price_rules (id) ON DELETE CASCADE
);

CREATE INDEX idx_price_rules_is_default ON tb_price_rules (is_default);
CREATE INDEX idx_price_rules_is_active ON tb_price_rules (is_active);
CREATE INDEX idx_price_rules_priority ON tb_price_rules (priority);
CREATE INDEX idx_price_rules_start_time ON tb_price_rules (start_time);
CREATE INDEX idx_price_rules_end_time ON tb_price_rules (end_time);
CREATE INDEX idx_price_rule_days_price_rule_id ON tb_price_rule_days (price_rule_id);
