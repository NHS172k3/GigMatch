"""
train_model.py
Trains a logistic regression on the synthetic GigMatch training data and exports
the model coefficients as JSON for use by the Java QualityScoreModel.

Usage:
  python generate_data.py   # run first to create training_data.csv
  python train_model.py     # trains and outputs quality_score_coefficients.json

The JSON is copied to backend/src/main/resources/quality_score_coefficients.json
so it is loaded at Spring Boot startup — no Python needed at runtime.
"""

import json
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, roc_auc_score
import shutil
import os

# ---- Load data ----
df = pd.read_csv("training_data.csv")
FEATURES = ["completion_rate", "avg_rating_norm", "skills_match_score"]
X = df[FEATURES].values
y = df["job_success"].values

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# ---- Train ----
model = LogisticRegression(max_iter=1000, random_state=42)
model.fit(X_train, y_train)

# ---- Evaluate ----
y_pred      = model.predict(X_test)
y_pred_prob = model.predict_proba(X_test)[:, 1]
accuracy    = accuracy_score(y_test, y_pred)
auc         = roc_auc_score(y_test, y_pred_prob)
print(f"Test Accuracy: {accuracy:.4f}")
print(f"Test AUC:      {auc:.4f}")

# ---- Export coefficients ----
coef = model.coef_[0]
output = {
    "coefficients": {
        "completion_rate":    float(coef[0]),
        "avg_rating_norm":    float(coef[1]),
        "skills_match_score": float(coef[2]),
    },
    "intercept": float(model.intercept_[0]),
    "description": (
        "Logistic regression predicting P(job_success). "
        "P(job completed on time and rated >= 4). "
        "Features: completion_rate (0-1), avg_rating_norm (rating/5.0), "
        "skills_match_score (fraction of required skills covered by provider, 0-1). "
        f"Test AUC: {auc:.4f}, Accuracy: {accuracy:.4f}."
    ),
    "metadata": {
        "training_samples": int(len(X_train)),
        "test_samples":     int(len(X_test)),
        "accuracy":         float(accuracy),
        "auc":              float(auc),
    }
}

OUTPUT_FILE = "quality_score_coefficients.json"
with open(OUTPUT_FILE, "w") as f:
    json.dump(output, f, indent=2)
print(f"\nCoefficients written to {OUTPUT_FILE}")
print(json.dumps(output["coefficients"], indent=2))
print(f"Intercept: {output['intercept']:.4f}")

# ---- Copy to backend resources ----
BACKEND_RESOURCES = os.path.join(
    os.path.dirname(__file__),
    "..", "backend", "src", "main", "resources", "quality_score_coefficients.json"
)
BACKEND_RESOURCES = os.path.normpath(BACKEND_RESOURCES)
try:
    shutil.copy(OUTPUT_FILE, BACKEND_RESOURCES)
    print(f"\nCopied to {BACKEND_RESOURCES}")
except FileNotFoundError:
    print(f"\nWarning: Could not copy to {BACKEND_RESOURCES} — copy manually if needed.")
