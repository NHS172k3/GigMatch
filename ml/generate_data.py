"""
generate_data.py
Generates synthetic training data for the GigMatch quality score model.

Features:
  - completion_rate:    historical job completion rate (0.6 to 1.0)
  - avg_rating_norm:    average client rating normalised to [0,1] = rating/5.0
  - skills_match_score: fraction of required skills the provider covers (0.0 to 1.0)

Label: job_success (1 = completed on time and rated >= 4, 0 = otherwise)

The true probability is a sigmoid of the linear combination used as ground truth.
Run this before train_model.py.
"""

import numpy as np
import pandas as pd

# Reproducibility
np.random.seed(42)
N = 10_000

# Feature distributions
completion_rate    = np.random.uniform(0.60, 1.00, N)
avg_rating_raw     = np.random.uniform(3.00, 5.00, N)
avg_rating_norm    = avg_rating_raw / 5.0
skills_match_score = np.random.uniform(0.00, 1.00, N)

# Ground truth: logistic model with known coefficients
TRUE_COEF_CR  = 1.8
TRUE_COEF_RN  = 1.4
TRUE_COEF_SM  = 0.9
TRUE_INTERCEPT = -2.3

logit = (TRUE_INTERCEPT
         + TRUE_COEF_CR * completion_rate
         + TRUE_COEF_RN * avg_rating_norm
         + TRUE_COEF_SM * skills_match_score
         + np.random.normal(0, 0.1, N))  # small noise

prob = 1 / (1 + np.exp(-logit))

# Sample binary labels
job_success = np.random.binomial(1, prob)

df = pd.DataFrame({
    "completion_rate":    completion_rate,
    "avg_rating_norm":    avg_rating_norm,
    "skills_match_score": skills_match_score,
    "job_success":        job_success,
})

df.to_csv("training_data.csv", index=False)

print(f"Generated {N} samples.")
print(f"  Success rate: {job_success.mean():.2%}")
print(f"  Feature means — CR: {completion_rate.mean():.3f}, "
      f"RN: {avg_rating_norm.mean():.3f}, SM: {skills_match_score.mean():.3f}")
print("Saved to training_data.csv")
