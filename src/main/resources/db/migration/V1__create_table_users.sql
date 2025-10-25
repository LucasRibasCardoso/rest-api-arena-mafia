CREATE TABLE tb_users
(
    id            UUID PRIMARY KEY,
    user_name     VARCHAR(50) UNIQUE NOT NULL,
    full_name     VARCHAR(255)       NOT NULL,
    phone         VARCHAR(20) UNIQUE NOT NULL,
    password_hash TEXT               NOT NULL,
    status        VARCHAR(30)        NOT NULL,
    role          VARCHAR(20)        NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_full_name ON tb_users (full_name);

CREATE INDEX idx_users_created_at ON tb_users (created_at);