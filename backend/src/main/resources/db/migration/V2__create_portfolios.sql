CREATE TABLE portfolios (
    id          BIGSERIAL    PRIMARY KEY,
    provider_id BIGINT       NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    sample_url  VARCHAR(500),
    category    VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_portfolios_provider_id ON portfolios(provider_id);
CREATE INDEX idx_portfolios_category    ON portfolios(category);
