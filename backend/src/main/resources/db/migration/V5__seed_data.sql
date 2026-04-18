-- Seed 5 providers matching the 5 simulated bidder beans

INSERT INTO providers (id, name, provider_key, avg_rating, completion_rate, daily_job_capacity, status)
VALUES
  (1, 'Expert Dev Studio',     'expert-dev',   4.9, 0.97, 8,  'ACTIVE'),
  (2, 'DesignPro Creative',    'design-pro',   4.7, 0.94, 10, 'ACTIVE'),
  (3, 'Generalist Works',      'generalist',   4.2, 0.88, 15, 'ACTIVE'),
  (4, 'Budget Builders',       'budget',       3.8, 0.82, 20, 'ACTIVE'),
  (5, 'DataScience Experts',   'data-science', 5.0, 0.99, 5,  'ACTIVE');

-- Seed skill offerings
INSERT INTO skill_offerings (provider_id, skill_category, min_quote_cents, max_quote_cents)
VALUES
  (1, 'software-dev', 15000, 30000),
  (1, 'backend',      15000, 30000),
  (1, 'api',          12000, 25000),
  (2, 'design',        8000, 20000),
  (2, 'branding',      8000, 20000),
  (2, 'ui-ux',         8000, 20000),
  (3, 'general',       5000, 15000),
  (3, 'writing',       3000, 10000),
  (3, 'admin',         2000,  8000),
  (4, 'general',       2000,  8000),
  (4, 'writing',       2000,  7000),
  (5, 'data-science',  20000, 50000),
  (5, 'ml',            20000, 50000),
  (5, 'analytics',     15000, 40000);

-- Seed portfolio items
INSERT INTO portfolios (provider_id, title, description, sample_url, category)
VALUES
  (1, 'E-Commerce REST API',        'Full REST API for an online store using Spring Boot',          'https://placehold.co/600x400?text=Expert+Dev', 'software-dev'),
  (2, 'Brand Identity Package',     'Complete brand identity including logo, colors, typography',   'https://placehold.co/600x400?text=Design+Pro', 'design'),
  (3, 'Content Writing Portfolio',  '50+ blog posts and marketing copy across various industries',  'https://placehold.co/600x400?text=Generalist', 'writing'),
  (4, 'Startup Landing Page',       'Clean, conversion-optimised landing page built fast',          'https://placehold.co/600x400?text=Budget', 'general'),
  (5, 'Customer Churn Prediction',  'ML pipeline predicting churn with 92% accuracy using Python', 'https://placehold.co/600x400?text=DataScience', 'data-science');

-- Reset sequence to avoid PK conflicts after explicit inserts
SELECT setval('providers_id_seq', (SELECT MAX(id) FROM providers));
