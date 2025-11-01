# -*- coding: utf-8 -*-
"""
Convert Keras H5 Federated Learning model to PyTorch Mobile format

This script:
1. Loads the Keras H5 model
2. Converts it to PyTorch
3. Exports to TorchScript format (.pt) for mobile deployment
4. Preserves model architecture and weights

Usage:
    python convert_fl_to_pytorch.py

Requirements:
    pip install torch tensorflow h5py
"""

import torch
import torch.nn as nn
import tensorflow as tf
import numpy as np
import os

class FLModelPyTorch(nn.Module):
    """
    PyTorch version of Federated Learning HAR model.
    Architecture matches the Keras model from federated_learning_training.ipynb:
    - Input: 561 features
    - Hidden Layer 1: Dense(512, activation='relu') + BatchNorm + Dropout(0.5)
    - Hidden Layer 2: Dense(256, activation='relu') + BatchNorm + Dropout(0.4)
    - Hidden Layer 3: Dense(128, activation='relu') + BatchNorm + Dropout(0.3)
    - Hidden Layer 4: Dense(64, activation='relu') + BatchNorm + Dropout(0.2)
    - Output Layer: Dense(6) + Softmax
    
    IMPORTANT: In Keras, Dense has activation='relu' inside it, so the order is:
    Linear → ReLU → BatchNorm → Dropout (NOT Linear → BatchNorm → ReLU!)
    """
    def __init__(self, input_size=561, num_classes=6):
        super(FLModelPyTorch, self).__init__()
        
        # First hidden layer (561 -> 512)
        self.fc1 = nn.Linear(input_size, 512)
        self.relu1 = nn.ReLU()
        self.bn1 = nn.BatchNorm1d(512)
        self.dropout1 = nn.Dropout(0.5)
        
        # Second hidden layer (512 -> 256)
        self.fc2 = nn.Linear(512, 256)
        self.relu2 = nn.ReLU()
        self.bn2 = nn.BatchNorm1d(256)
        self.dropout2 = nn.Dropout(0.4)
        
        # Third hidden layer (256 -> 128)
        self.fc3 = nn.Linear(256, 128)
        self.relu3 = nn.ReLU()
        self.bn3 = nn.BatchNorm1d(128)
        self.dropout3 = nn.Dropout(0.3)
        
        # Fourth hidden layer (128 -> 64)
        self.fc4 = nn.Linear(128, 64)
        self.relu4 = nn.ReLU()
        self.bn4 = nn.BatchNorm1d(64)
        self.dropout4 = nn.Dropout(0.2)
        
        # Output layer (64 -> 6)
        self.fc5 = nn.Linear(64, num_classes)
        
    def forward(self, x):
        # Layer 1: Linear → ReLU → BatchNorm → Dropout
        x = self.fc1(x)
        x = self.relu1(x)
        x = self.bn1(x)
        x = self.dropout1(x)
        
        # Layer 2: Linear → ReLU → BatchNorm → Dropout
        x = self.fc2(x)
        x = self.relu2(x)
        x = self.bn2(x)
        x = self.dropout2(x)
        
        # Layer 3: Linear → ReLU → BatchNorm → Dropout
        x = self.fc3(x)
        x = self.relu3(x)
        x = self.bn3(x)
        x = self.dropout3(x)
        
        # Layer 4: Linear → ReLU → BatchNorm → Dropout
        x = self.fc4(x)
        x = self.relu4(x)
        x = self.bn4(x)
        x = self.dropout4(x)
        
        # Output: Linear (no softmax - use raw logits)
        x = self.fc5(x)
        return x

def load_keras_model(h5_path):
    """Load Keras H5 model"""
    print(f"Loading Keras model from {h5_path}...")
    keras_model = tf.keras.models.load_model(h5_path)
    print("Keras model architecture:")
    keras_model.summary()
    return keras_model

def convert_weights(keras_model, pytorch_model):
    """
    Convert weights from Keras to PyTorch.
    Maps all 5 Dense layers and 4 BatchNorm layers.
    
    Keras Model Architecture (from training notebook):
    - Layer 0: Dense(512) 
    - Layer 1: BatchNorm
    - Layer 2: Dense(256)
    - Layer 3: BatchNorm
    - Layer 4: Dense(128)
    - Layer 5: BatchNorm
    - Layer 6: Dense(64)
    - Layer 7: BatchNorm
    - Layer 8: Dense(6)
    """
    print("\nConverting weights from Keras to PyTorch...")
    
    keras_layers = [layer for layer in keras_model.layers if len(layer.get_weights()) > 0]
    print(f"Found {len(keras_layers)} Keras layers with weights")
    
    # Map Keras layers to PyTorch layers
    # Format: keras_index: (pytorch_layer_name, has_bias)
    layer_mapping = {
        0: ('fc1', True),   # Dense(561 -> 512)
        1: ('bn1', False),  # BatchNorm(512)
        2: ('fc2', True),   # Dense(512 -> 256)
        3: ('bn2', False),  # BatchNorm(256)
        4: ('fc3', True),   # Dense(256 -> 128)
        5: ('bn3', False),  # BatchNorm(128)
        6: ('fc4', True),   # Dense(128 -> 64)
        7: ('bn4', False),  # BatchNorm(64)
        8: ('fc5', True),   # Dense(64 -> 6) - Output layer
    }
    
    with torch.no_grad():
        for keras_idx, (pytorch_name, has_bias) in layer_mapping.items():
            if keras_idx >= len(keras_layers):
                print(f"Warning: Keras layer {keras_idx} not found")
                continue
                
            keras_layer = keras_layers[keras_idx]
            weights = keras_layer.get_weights()
            
            pytorch_layer = getattr(pytorch_model, pytorch_name)
            
            if isinstance(pytorch_layer, nn.Linear):
                # Dense/Linear layer: transpose weights (Keras is transposed vs PyTorch)
                weight_tensor = torch.from_numpy(weights[0].T).float()
                pytorch_layer.weight.copy_(weight_tensor)
                
                if has_bias and len(weights) > 1:
                    bias_tensor = torch.from_numpy(weights[1]).float()
                    pytorch_layer.bias.copy_(bias_tensor)
                    
                print(f"✓ Converted {pytorch_name}: weight shape {weight_tensor.shape}")
                
            elif isinstance(pytorch_layer, nn.BatchNorm1d):
                # BatchNorm layer: gamma, beta, mean, variance
                gamma = torch.from_numpy(weights[0]).float()  # scale
                beta = torch.from_numpy(weights[1]).float()   # shift
                mean = torch.from_numpy(weights[2]).float()   # running mean
                var = torch.from_numpy(weights[3]).float()    # running variance
                
                pytorch_layer.weight.copy_(gamma)
                pytorch_layer.bias.copy_(beta)
                pytorch_layer.running_mean.copy_(mean)
                pytorch_layer.running_var.copy_(var)
                
                print(f"✓ Converted {pytorch_name}: BatchNorm")
    
    print("Weight conversion complete!")

def export_to_mobile(pytorch_model, output_path):
    """
    Export PyTorch model to TorchScript mobile format
    
    IMPORTANT: We use torch.jit.script() and DO NOT use optimize_for_inference()
    because optimize_for_inference() folds BatchNorm layers into constants,
    making the model unresponsive to input changes.
    """
    print(f"\nExporting to TorchScript mobile format...")
    
    # Set model to evaluation mode
    pytorch_model.eval()
    
    # Use torch.jit.script() to preserve model structure
    # This ensures BatchNorm uses stored statistics correctly
    try:
        scripted_model = torch.jit.script(pytorch_model)
        print("✓ Model scripted successfully (preserves BatchNorm)")
    except Exception as e:
        print(f"⚠ Warning: torch.jit.script() failed: {e}")
        print("  Falling back to trace method...")
        example_input = torch.randn(1, 561)
        scripted_model = torch.jit.trace(pytorch_model, example_input)
    
    # DO NOT use optimize_for_inference() - it breaks BatchNorm!
    # The model will be slightly larger but will actually work correctly
    
    # Save the model
    scripted_model.save(output_path)
    
    print(f"✓ Model exported to {output_path}")
    print(f"  File size: {os.path.getsize(output_path) / 1024:.2f} KB")

def verify_conversion(keras_model, pytorch_model):
    """
    Verify that converted model produces same outputs as Keras
    Tests with batch size > 1 to avoid BatchNorm issues
    """
    print("\nVerifying conversion...")
    pytorch_model.eval()
    
    # Use batch size = 10 to avoid BatchNorm issues with batch_size=1
    batch_size = 10
    test_cases = [
        ("Random input", np.random.randn(batch_size, 561).astype(np.float32)),
        ("All zeros", np.zeros((batch_size, 561), dtype=np.float32)),
        ("All ones", np.ones((batch_size, 561), dtype=np.float32)),
    ]
    
    all_passed = True
    
    for test_name, test_input in test_cases:
        # Keras prediction
        keras_output = keras_model.predict(test_input, verbose=0)
        
        # PyTorch prediction
        with torch.no_grad():
            pytorch_input = torch.from_numpy(test_input)
            pytorch_output = pytorch_model(pytorch_input).numpy()
        
        # Compare outputs
        diff = np.abs(keras_output - pytorch_output).max()
        
        if diff < 0.01:
            print(f"  ✓ {test_name}: diff = {diff:.6f}")
        else:
            print(f"  ❌ {test_name}: diff = {diff:.6f} (TOO LARGE!)")
            all_passed = False
    
    if all_passed:
        print("✓ Conversion verified successfully!")
        return True
    else:
        print("⚠ Warning: Large differences detected. Check layer mapping.")
        return False

def main():
    # Paths
    keras_model_path = "models/federated_learning_model.h5"
    pytorch_output_path = "models/fl_model.pt"
    
    # Check if Keras model exists
    if not os.path.exists(keras_model_path):
        print(f"Error: Keras model not found at {keras_model_path}")
        print("Please ensure the model file exists in the correct location.")
        return
    
    # Create output directory
    os.makedirs("models", exist_ok=True)
    
    # Load Keras model
    keras_model = load_keras_model(keras_model_path)
    
    # Create PyTorch model
    print("\nCreating PyTorch model...")
    pytorch_model = FLModelPyTorch()
    
    # Convert weights
    convert_weights(keras_model, pytorch_model)
    
    # Verify conversion
    verify_conversion(keras_model, pytorch_model)
    
    # Export to mobile format
    export_to_mobile(pytorch_model, pytorch_output_path)
    
    print("\n" + "="*60)
    print("Conversion complete!")
    print(f"PyTorch Mobile model saved to: {pytorch_output_path}")
    print("\nNext steps:")
    print("1. Copy fl_model.pt to your Android assets folder")
    print("2. Update PyTorchFLService to load the model")
    print("3. Test on-device inference")
    print("="*60)

if __name__ == "__main__":
    main()
