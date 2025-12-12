"""
Debug script to check if scalers are loaded and working correctly
"""
import os
import sys
import joblib
import numpy as np

print("=" * 80)
print("🔍 Checking Scaler Files")
print("=" * 80)

# Check scaler files
scaler_dl_path = "models/scaler_dl.pkl"
scaler_fl_path = "models/scaler_fl.pkl"

print(f"\n1. Checking DL Scaler: {scaler_dl_path}")
if os.path.exists(scaler_dl_path):
    print(f"   ✅ File exists")
    try:
        scaler_dl = joblib.load(scaler_dl_path)
        print(f"   ✅ Loaded successfully")
        print(f"   Type: {type(scaler_dl)}")
        print(f"   Mean shape: {scaler_dl.mean_.shape}")
        print(f"   Scale shape: {scaler_dl.scale_.shape}")
        print(f"   Mean sample (first 5): {scaler_dl.mean_[:5]}")
        print(f"   Scale sample (first 5): {scaler_dl.scale_[:5]}")
    except Exception as e:
        print(f"   ❌ Error loading: {e}")
else:
    print(f"   ❌ File NOT found!")

print(f"\n2. Checking FL Scaler: {scaler_fl_path}")
if os.path.exists(scaler_fl_path):
    print(f"   ✅ File exists")
    try:
        scaler_fl = joblib.load(scaler_fl_path)
        print(f"   ✅ Loaded successfully")
        print(f"   Type: {type(scaler_fl)}")
        print(f"   Mean shape: {scaler_fl.mean_.shape}")
        print(f"   Scale shape: {scaler_fl.scale_.shape}")
        print(f"   Mean sample (first 5): {scaler_fl.mean_[:5]}")
        print(f"   Scale sample (first 5): {scaler_fl.scale_[:5]}")
    except Exception as e:
        print(f"   ❌ Error loading: {e}")
else:
    print(f"   ❌ File NOT found!")

# Test preprocessing
print("\n" + "=" * 80)
print("🧪 Testing Preprocessing")
print("=" * 80)

if os.path.exists(scaler_dl_path):
    from utils.preprocessing import preprocess_data
    
    # Create test data (random 561 features)
    test_data = np.random.randn(561).astype(np.float32)
    print(f"\n3. Test data created")
    print(f"   Shape: {test_data.shape}")
    print(f"   Range: [{test_data.min():.4f}, {test_data.max():.4f}]")
    print(f"   Mean: {test_data.mean():.4f}")
    
    # Test without scaler
    print(f"\n4. Processing WITHOUT scaler")
    processed_without = preprocess_data(test_data, scaler=None)
    print(f"   Shape: {processed_without.shape}")
    print(f"   Range: [{processed_without.min():.4f}, {processed_without.max():.4f}]")
    print(f"   Mean: {processed_without.mean():.4f}")
    
    # Test with scaler
    print(f"\n5. Processing WITH scaler")
    scaler_dl = joblib.load(scaler_dl_path)
    processed_with = preprocess_data(test_data, scaler=scaler_dl)
    print(f"   Shape: {processed_with.shape}")
    print(f"   Range: [{processed_with.min():.4f}, {processed_with.max():.4f}]")
    print(f"   Mean: {processed_with.mean():.4f}")
    
    print(f"\n6. Difference check")
    print(f"   Are they different? {not np.allclose(processed_without, processed_with)}")
    if not np.allclose(processed_without, processed_with):
        print(f"   ✅ GOOD! Scaler is making a difference")
        print(f"   Max difference: {np.abs(processed_without - processed_with).max():.4f}")
    else:
        print(f"   ❌ BAD! Scaler not having any effect")

print("\n" + "=" * 80)
print("✅ Debug Complete")
print("=" * 80)
