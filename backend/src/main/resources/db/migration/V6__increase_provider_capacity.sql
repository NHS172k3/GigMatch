-- Raise daily job capacity to realistic demo values.
-- The original seed values (5-20) were exhausted in seconds under load testing,
-- causing all subsequent auctions to return NO_MATCH.
UPDATE providers SET daily_job_capacity = 500 WHERE provider_key = 'expert-dev';
UPDATE providers SET daily_job_capacity = 600 WHERE provider_key = 'design-pro';
UPDATE providers SET daily_job_capacity = 800 WHERE provider_key = 'generalist';
UPDATE providers SET daily_job_capacity = 1000 WHERE provider_key = 'budget';
UPDATE providers SET daily_job_capacity = 300 WHERE provider_key = 'data-science';
