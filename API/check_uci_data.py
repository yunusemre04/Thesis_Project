"""
Check UCI HAR test data to understand expected feature ranges
"""
import numpy as np
import os

print("=" * 80)
print("🔍 UCI HAR Test Data Analysis")
print("=" * 80)

# Load test data
X_test_path = "UCI_HAR_Dataset/test/X_test.txt"
y_test_path = "UCI_HAR_Dataset/test/y_test.txt"

if os.path.exists(X_test_path):
    X_test = np.loadtxt(X_test_path)
    y_test = np.loadtxt(y_test_path, dtype=int)
    
    print(f"\n✅ UCI HAR Test Data Loaded")
    print(f"   Samples: {X_test.shape[0]}")
    print(f"   Features: {X_test.shape[1]}")
    print(f"   Labels: {y_test.shape[0]}")
    
    print(f"\n📊 Feature Statistics (BEFORE preprocessing)")
    print(f"   Min: {X_test.min():.6f}")
    print(f"   Max: {X_test.max():.6f}")
    print(f"   Mean: {X_test.mean():.6f}")
    print(f"   Std: {X_test.std():.6f}")
    print(f"   Median: {np.median(X_test):.6f}")
    
    print(f"\n📊 Per-Feature Statistics (first 10 features)")
    for i in range(10):
        feat = X_test[:, i]
        print(f"   Feature {i}: min={feat.min():.4f}, max={feat.max():.4f}, mean={feat.mean():.4f}, std={feat.std():.4f}")
    
    print(f"\n📊 Feature Distribution")
    print(f"   Features with values > 1: {np.sum(np.abs(X_test) > 1)} / {X_test.size} ({100 * np.sum(np.abs(X_test) > 1) / X_test.size:.2f}%)")
    print(f"   Features with values > 10: {np.sum(np.abs(X_test) > 10)} / {X_test.size} ({100 * np.sum(np.abs(X_test) > 10) / X_test.size:.2f}%)")
    
    # Check a single sample
    sample_0 = X_test[0]
    print(f"\n📊 Sample 0 (label={y_test[0]-1})")
    print(f"   Min: {sample_0.min():.6f}")
    print(f"   Max: {sample_0.max():.6f}")
    print(f"   Mean: {sample_0.mean():.6f}")
    print(f"   Non-zero: {np.count_nonzero(sample_0)} / {len(sample_0)}")
    print(f"   First 10 features: {sample_0[:10]}")
    
    # Now apply scaler
    import joblib
    from utils.preprocessing import preprocess_data
    
    scaler = joblib.load("models/scaler_dl.pkl")
    processed = preprocess_data(sample_0, scaler=scaler)
    
    print(f"\n📊 Sample 0 (AFTER preprocessing with scaler)")
    print(f"   Min: {processed.min():.6f}")
    print(f"   Max: {processed.max():.6f}")
    print(f"   Mean: {processed.mean():.6f}")
    print(f"   First 10 features: {processed[0][:10]}")
    
else:
    print(f"❌ UCI HAR Dataset not found at {X_test_path}")

print("\n" + "=" * 80)
