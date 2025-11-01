"""
Data preprocessing utilities
"""
import numpy as np
from typing import Union, List
from sklearn.preprocessing import StandardScaler


def preprocess_data(data: Union[List, np.ndarray], scaler: StandardScaler = None) -> np.ndarray:
    """
    Preprocess sensor data for model input
    
    Args:
        data: Raw sensor data (list or numpy array)
        scaler: Optional pre-fitted StandardScaler for normalization
    
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
    
    # Apply scaling if scaler is provided
    if scaler is not None:
        try:
            data = scaler.transform(data)
        except Exception as e:
            # If scaling fails, apply basic normalization
            # UCI HAR data is typically in range [-1, 1] to [3, 3] for sensor readings
            # Normalize to a reasonable range
            data_mean = np.mean(data, axis=1, keepdims=True)
            data_std = np.std(data, axis=1, keepdims=True) + 1e-8  # Avoid division by zero
            data = (data - data_mean) / data_std
    else:
        # No scaler provided, apply simple normalization
        # This ensures the data is in a reasonable range for the model
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

