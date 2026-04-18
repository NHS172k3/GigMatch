CREATE TABLE skill_offerings (
    id              BIGSERIAL    PRIMARY KEY,
    provider_id     BIGINT       NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    skill_category  VARCHAR(100) NOT NULL,
    min_quote_cents INT          NOT NULL,
    max_quote_cents INT          NOT NULL
);

CREATE INDEX idx_skill_offerings_provider_id ON skill_offerings(provider_id);
CREATE INDEX idx_skill_offerings_category    ON skill_offerings(skill_category);
