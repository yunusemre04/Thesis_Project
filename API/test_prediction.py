"""
Test script to validate Deep Learning API predictions using real UCI HAR test data
This ensures the API + model + preprocessing pipeline works correctly
"""
import requests
import numpy as np
import os

# API endpoint
api_url = "http://10.69.117.218:5000/api/dl/predict"

# Load UCI HAR test data
def load_test_sample(sample_idx=0):
    """Load a single test sample from UCI HAR dataset"""
    dataset_path = "UCI_HAR_Dataset"
    
    # Load test features
    X_test_path = os.path.join(dataset_path, "test", "X_test.txt")
    y_test_path = os.path.join(dataset_path, "test", "y_test.txt")
    
    # Read data
    X_test = np.loadtxt(X_test_path)
    y_test = np.loadtxt(y_test_path, dtype=int)
    
    # Activity labels (1-indexed in file, convert to 0-indexed)
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
print("🧪 UCI HAR API Test - Validating Predictions")
print("="*80)

# Test multiple samples
correct = 0
total = 10

for i in range(total):
    features, true_label, activity_labels = load_test_sample(i)
    true_activity = activity_labels[true_label]
    
    print(f"\n📊 Test {i+1}/{total}")
    print(f"  True Activity: {true_activity} (class {true_label})")
    print(f"  Features: {features.shape[0]} values")
    print(f"  Feature range: [{features.min():.4f}, {features.max():.4f}]")
    
    # Make prediction
    try:
        response = requests.post(api_url, json={"sensor_data": features.tolist()})
        
        if response.status_code == 200:
            result = response.json()
            predicted_activity = result['prediction']['activity']
            confidence = result['prediction']['confidence']
            
            is_correct = predicted_activity == true_activity
            correct += is_correct
            
            status = "✅ CORRECT" if is_correct else "❌ WRONG"
            print(f"  {status}")
            print(f"  Predicted: {predicted_activity} ({confidence}%)")
            
            if not is_correct:
                probs = result['prediction']['all_probabilities']
                sorted_probs = sorted(probs.items(), key=lambda x: x[1], reverse=True)[:3]
                print(f"  Top 3:")
                for act, prob in sorted_probs:
                    print(f"    {act}: {prob}%")
        else:
            print(f"  ❌ API Error: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"  ❌ Request failed: {str(e)}")

print("\n" + "="*80)
print(f"Accuracy: {correct}/{total} = {100*correct/total:.1f}%")
print("="*80)

# Now test with ALL ZEROS to see what happens
print("\n" + "="*80)
print("Testing with ALL ZEROS (simulating bad feature extraction)")
print("="*80)

zero_features = [0.0] * 561
try:
    response = requests.post(api_url, json={"sensor_data": zero_features})
    if response.status_code == 200:
        result = response.json()
        print(f"Prediction: {result['prediction']['activity']} ({result['prediction']['confidence']}%)")
        probs = result['prediction']['all_probabilities']
        sorted_probs = sorted(probs.items(), key=lambda x: x[1], reverse=True)
        for act, prob in sorted_probs:
            print(f"  {act}: {prob}%")
        print("\n⚠️  This is what happens with zero features - likely wrong!")
    else:
        print(f"API Error: {response.status_code}")
except Exception as e:
    print(f"Request failed: {str(e)}")
