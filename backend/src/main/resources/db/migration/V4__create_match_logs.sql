CREATE TABLE match_logs (
    id                      BIGSERIAL    PRIMARY KEY,
    request_id              VARCHAR(36)  NOT NULL,
    client_id               VARCHAR(100),
    job_category            VARCHAR(100),
    required_skills         VARCHAR(1000),
    budget_cents            INT,
    urgency_level           VARCHAR(20),
    winner_provider_key     VARCHAR(100),
    winner_provider_id      BIGINT,
    clearing_price_cents    INT,
    predicted_success_rate  DOUBLE PRECISION,
    effective_score         DOUBLE PRECISION,
    duration_ms             BIGINT       NOT NULL,
    outcome                 VARCHAR(20)  NOT NULL,
    participating_providers VARCHAR(500),
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_logs_created_at        ON match_logs(created_at DESC);
CREATE INDEX idx_match_logs_winner_provider   ON match_logs(winner_provider_id);
CREATE INDEX idx_match_logs_outcome           ON match_logs(outcome);
CREATE INDEX idx_match_logs_job_category      ON match_logs(job_category);
