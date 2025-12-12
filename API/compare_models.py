"""
Compare DL vs FL/PyTorch predictions on the same UCI HAR test samples
This will help identify where the discrepancy is
"""
import requests
import numpy as np
import os

# API endpoints
dl_api_url = "http://10.69.117.218:5000/api/dl/predict"
fl_api_url = "http://10.69.117.218:5000/api/fl/predict"

def load_test_sample(sample_idx=0):
    """Load a single test sample from UCI HAR dataset"""
    dataset_path = "UCI_HAR_Dataset"
    
    X_test_path = os.path.join(dataset_path, "test", "X_test.txt")
    y_test_path = os.path.join(dataset_path, "test", "y_test.txt")
    
    X_test = np.loadtxt(X_test_path)
    y_test = np.loadtxt(y_test_path, dtype=int)
    
    activity_labels = [
        'WALKING',
        'WALKING_UPSTAIRS', 
        'WALKING_DOWNSTAIRS',
        'SITTING',
        'STANDING',
        'LAYING'
    ]
    
    return X_test[sample_idx], y_test[sample_idx] - 1, activity_labels

print("="*80)
print("🔍 DL vs FL Prediction Comparison")
print("="*80)

# Test 5 samples
for i in range(5):
    features, true_label, activity_labels = load_test_sample(i)
    true_activity = activity_labels[true_label]
    
    print(f"\n{'='*80}")
    print(f"📊 Sample {i}")
    print(f"{'='*80}")
    print(f"True Activity: {true_activity} (class {true_label})")
    print(f"Feature range: [{features.min():.4f}, {features.max():.4f}]")
    print(f"Feature mean: {features.mean():.4f}")
    print(f"First 10 features: {features[:10]}")
    
    # Test DL model
    try:
        response = requests.post(dl_api_url, json={"sensor_data": features.tolist()})
        if response.status_code == 200:
            result = response.json()
            dl_pred = result['prediction']['activity']
            dl_conf = result['prediction']['confidence']
            dl_correct = "✅" if dl_pred == true_activity else "❌"
            
            print(f"\n🔵 DL Model:")
            print(f"   {dl_correct} Predicted: {dl_pred} ({dl_conf:.2f}%)")
            
            # Show top 3
            probs = result['prediction']['all_probabilities']
            sorted_probs = sorted(probs.items(), key=lambda x: x[1], reverse=True)[:3]
            for act, prob in sorted_probs:
                print(f"      {act}: {prob:.2f}%")
        else:
            print(f"\n🔵 DL Model: ❌ Error {response.status_code}")
            print(f"   {response.text}")
    except Exception as e:
        print(f"\n🔵 DL Model: ❌ Exception: {str(e)}")
    
    # Test FL model
    try:
        response = requests.post(fl_api_url, json={"sensor_data": features.tolist()})
        if response.status_code == 200:
            result = response.json()
            fl_pred = result['prediction']['activity']
            fl_conf = result['prediction']['confidence']
            fl_correct = "✅" if fl_pred == true_activity else "❌"
            
            print(f"\n🟢 FL Model:")
            print(f"   {fl_correct} Predicted: {fl_pred} ({fl_conf:.2f}%)")
            
            # Show top 3
            probs = result['prediction']['all_probabilities']
            sorted_probs = sorted(probs.items(), key=lambda x: x[1], reverse=True)[:3]
            for act, prob in sorted_probs:
                print(f"      {act}: {prob:.2f}%")
        else:
            print(f"\n🟢 FL Model: ❌ Error {response.status_code}")
            print(f"   {response.text}")
    except Exception as e:
        print(f"\n🟢 FL Model: ❌ Exception: {str(e)}")
    
    # Compare
    if 'dl_pred' in locals() and 'fl_pred' in locals():
        if dl_pred == fl_pred:
            print(f"\n✅ AGREEMENT: Both models predict {dl_pred}")
        else:
            print(f"\n⚠️  DISAGREEMENT: DL={dl_pred}, FL={fl_pred}")

print("\n" + "="*80)
print("Test Complete")
print("="*80)
