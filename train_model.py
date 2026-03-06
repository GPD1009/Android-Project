import pandas as pd
import numpy as np
import joblib
import os
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import RandomForestClassifier

# List of all CIC-IDS2017 CSV files
FILES = [
    "CIC-IDS2017(Monday).csv",
    "CIC-IDS2017(Tuesday).csv",
    "CIC-IDS2017(Wednesday).csv",
    "CIC-IDS2017(Thursday Morning).csv",
    "CIC-IDS2017(Thursday Afternoon).csv",
    "CIC-IDS2017(Friday Morning).csv",
    "CIC-IDS2017(Friday Afternoon).csv",
    "CIC-IDS2017(Friday Afternoon 2).csv"
]

def clean_dataset(df):
    """Removes Infinity and NaN values and drops non-numeric columns."""
    # 1. Clean column names (strip spaces)
    df.columns = df.columns.str.strip()
    
    # 2. Drop non-numeric columns that cause training errors
    # These are network identifiers, not behavior patterns.
    cols_to_drop = ['Flow ID', 'Source IP', 'Source Port', 'Destination IP', 'Destination Port', 'Protocol', 'Timestamp']
    df = df.drop(columns=[c for c in cols_to_drop if c in df.columns], errors='ignore')
    
    # 3. Handle Infinity and NaNs
    df.replace([np.inf, -np.inf], np.nan, inplace=True)
    df.dropna(inplace=True)
    
    return df

print("--- Phase 1: Merging Datasets ---")
dfs = []
for file in FILES:
    if os.path.exists(file):
        print(f"Loading {file}...")
        # Loading only necessary columns to save RAM
        temp_df = pd.read_csv(file)
        temp_df = clean_dataset(temp_df)
        dfs.append(temp_df)
    else:
        print(f"⚠️ Warning: {file} not found. Skipping.")

if not dfs:
    print("❌ Error: No CSV files found. Please place them in the script folder.")
    exit()

master_df = pd.concat(dfs, ignore_index=True)
print(f"Total Rows Loaded: {len(master_df)}")

print("\n--- Phase 2: Preprocessing ---")
# Separate Features and Label
X = master_df.drop("Label", axis=1)
y = master_df["Label"]

# Encode the Labels (Benign, Dos, PortScan, etc.)
le = LabelEncoder()
y_encoded = le.fit_transform(y)

# Scale the features
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# Split for validation
X_train, X_test, y_train, y_test = train_test_split(
    X_scaled, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
)

print("\n--- Phase 3: Training Random Forest ---")
# Using n_estimators=50 for a balance between accuracy and file size
rf_model = RandomForestClassifier(
    n_estimators=50,
    random_state=42,
    n_jobs=-1, # Uses all CPU cores
    verbose=1
)

rf_model.fit(X_train, y_train)

print("\n--- Phase 4: Saving Assets ---")
# These filenames MUST match what your Python Server looks for
joblib.dump(rf_model, "Ultimate_NIDS_Model.pkl")
joblib.dump(scaler, "Ultimate_Scaler.pkl")
joblib.dump(le, "Ultimate_LabelEncoder.pkl")

# Save a feature list for the server to verify input shape
joblib.dump(list(X.columns), "Feature_List.pkl")

print("\n✅ Success! All models saved.")
print(f"Classes found: {list(le.classes_)}")