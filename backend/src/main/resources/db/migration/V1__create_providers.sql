CREATE TABLE providers (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    provider_key      VARCHAR(100) NOT NULL UNIQUE,
    avg_rating        DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    completion_rate   DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    daily_job_capacity INT NOT NULL,
    total_active_jobs  INT NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_providers_status ON providers(status);
CREATE INDEX idx_providers_key    ON providers(provider_key);
