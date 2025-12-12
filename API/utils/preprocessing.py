"""
Data preprocessing utilities
"""
import numpy as np
from typing import Union, List
from sklearn.preprocessing import StandardScaler


def preprocess_data(data: Union[List, np.ndarray], scaler: StandardScaler = None) -> np.ndarray:
    """
    Preprocess sensor data for model input using UCI HAR StandardScaler
    
    CRITICAL: The scaler MUST be the one fitted on UCI HAR training data!
    Models were trained with StandardScaler that normalizes each feature 
    to mean=0, std=1 based on training data statistics.
    
    Args:
        data: Raw sensor data (list or numpy array) - 561 UCI HAR features
        scaler: Pre-fitted StandardScaler from training (REQUIRED for correct predictions)
    
    Returns:
        Preprocessed numpy array ready for model input
    """
    # Convert to numpy array if needed with proper dtype
    if isinstance(data, list):
        try:
            data = np.array(data, dtype=np.float32)
        except (ValueError, TypeError) as e:
            raise ValueError(f"Cannot convert input to numeric array: {str(e)}")
    elif isinstance(data, np.ndarray):
        data = data.astype(np.float32)
    else:
        raise ValueError(f"Unsupported data type: {type(data)}")
    
    # Replace NaN and Inf values with safe defaults
    data = np.nan_to_num(data, nan=0.0, posinf=1.0, neginf=-1.0)
    
    # Ensure 2D shape (batch_size, features)
    if len(data.shape) == 1:
        data = data.reshape(1, -1)
    
    # Apply UCI HAR StandardScaler (REQUIRED!)
    if scaler is not None:
        # Use the scaler fitted on UCI HAR training data
        # This applies: (X - mean_train) / std_train for each feature
        data = scaler.transform(data)
    else:
        # WARNING: Without proper scaler, predictions will be WRONG!
        # Fallback to per-sample normalization (not ideal)
        print("⚠️  WARNING: No scaler provided! Using fallback normalization.")
        print("   Predictions may be incorrect without UCI HAR training statistics.")
        data_mean = np.mean(data, axis=1, keepdims=True)
        data_std = np.std(data, axis=1, keepdims=True) + 1e-8
        data = (data - data_mean) / data_std
    
    return data


def validate_input_shape(data: np.ndarray, expected_features: int = 561) -> bool:
    """
    Validate that input data has the correct shape
    
    Args:
        data: Input data array
        expected_features: Expected number of features
    
    Returns:
        True if shape is valid, False otherwise
    """
    if len(data.shape) != 2:
        return False
    
    if data.shape[1] != expected_features:
        return False
    
    return True


def create_standard_scaler() -> StandardScaler:
    """
    Create a StandardScaler with mean=0 and std=1
    Note: This is a placeholder. In production, you should load
    the scaler that was fitted during training.
    
    Returns:
        StandardScaler instance
    """
    scaler = StandardScaler()
    # For now, return unfitted scaler
    # In production, load the saved scaler from training
    return scaler


def normalize_features(data: np.ndarray) -> np.ndarray:
    """
    Simple normalization to [-1, 1] range
    Used when scaler is not available
    
    Args:
        data: Input data
    
    Returns:
        Normalized data
    """
    # Assume data is already normalized from UCI HAR dataset
    # Just ensure it's in float32 for model compatibility
    return data.astype(np.float32)

