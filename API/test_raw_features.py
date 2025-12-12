"""
Test how the API handles raw (non-normalized) features from UCI HAR dataset
This simulates what the mobile app now sends (statistical features without [-1,1] normalization)
"""
import numpy as np
import sys
import os

# Add parent directory to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from services.model_manager import ModelManager
from utils.preprocessing import preprocess_data

# Load models and scalers
print("Loading models and scalers...")
from config import Config
cfg = Config()
config_dict = {
    'DL_MODEL_PATH': cfg.DL_MODEL_PATH,
    'FL_MODEL_PATH': cfg.FL_MODEL_PATH,
    'DL_SCALER_PATH': cfg.DL_SCALER_PATH,
    'FL_SCALER_PATH': cfg.FL_SCALER_PATH
}
model_manager = ModelManager(config_dict)
model_manager.load_models()

dl_model = model_manager.get_dl_model()
fl_model = model_manager.get_fl_model()
dl_scaler = model_manager.get_dl_scaler()
fl_scaler = model_manager.get_fl_scaler()

print(f"✅ DL Model loaded: {dl_model is not None}")
print(f"✅ FL Model loaded: {fl_model is not None}")
print(f"✅ DL Scaler loaded: {dl_scaler is not None}")
print(f"✅ FL Scaler loaded: {fl_scaler is not None}")
print()

# Load UCI HAR test data (raw features before any preprocessing)
data_path = 'UCI_HAR_Dataset/test/X_test.txt'
labels_path = 'UCI_HAR_Dataset/test/y_test.txt'

print(f"Loading UCI HAR test data from {data_path}...")
X_test = np.loadtxt(data_path)
y_test = np.loadtxt(labels_path, dtype=int)

print(f"✅ Loaded {len(X_test)} test samples")
print(f"   Feature shape: {X_test.shape}")
print(f"   Raw feature range: [{X_test.min():.3f}, {X_test.max():.3f}]")
print(f"   Raw feature mean: {X_test.mean():.3f}")
print()

# Activity labels
activity_labels = ['WALKING', 'WALKING_UPSTAIRS', 'WALKING_DOWNSTAIRS', 
                   'SITTING', 'STANDING', 'LAYING']

# Test with first 5 samples
print("Testing predictions with RAW UCI HAR features (no pre-normalization):")
print("=" * 80)

for i in range(5):
    features = X_test[i]
    true_label = activity_labels[y_test[i] - 1]
    
    print(f"\nSample {i}: True Activity = {true_label}")
    print(f"   Raw features: min={features.min():.3f}, max={features.max():.3f}, mean={features.mean():.3f}")
    
    # Test DL model
    features_array = np.array([features])
    preprocessed_dl = preprocess_data(features_array, dl_scaler)
    print(f"   After DL scaler: min={preprocessed_dl.min():.3f}, max={preprocessed_dl.max():.3f}, mean={preprocessed_dl.mean():.3f}")
    
    dl_probs = dl_model.predict(preprocessed_dl, verbose=0)[0]
    dl_pred_idx = np.argmax(dl_probs)
    dl_pred_label = activity_labels[dl_pred_idx]
    dl_confidence = dl_probs[dl_pred_idx] * 100
    
    # Test FL model (also TensorFlow H5 format)
    preprocessed_fl = preprocess_data(features_array, fl_scaler)
    print(f"   After FL scaler: min={preprocessed_fl.min():.3f}, max={preprocessed_fl.max():.3f}, mean={preprocessed_fl.mean():.3f}")
    
    fl_probs = fl_model.predict(preprocessed_fl, verbose=0)[0]
    fl_pred_idx = np.argmax(fl_probs)
    fl_pred_label = activity_labels[fl_pred_idx]
    fl_confidence = fl_probs[fl_pred_idx] * 100
    
    # Results
    dl_correct = "✅" if dl_pred_label == true_label else "❌"
    fl_correct = "✅" if fl_pred_label == true_label else "❌"
    agree = "✅ AGREEMENT" if dl_pred_label == fl_pred_label else "❌ DISAGREE"
    
    print(f"   DL: {dl_pred_label} ({dl_confidence:.2f}%) {dl_correct}")
    print(f"   FL: {fl_pred_label} ({fl_confidence:.2f}%) {fl_correct}")
    print(f"   {agree}")

print("\n" + "=" * 80)
print("✅ Test complete - these are predictions with RAW features (like mobile app now sends)")
