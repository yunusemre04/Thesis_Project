"""
Input validation utilities
"""
import numpy as np
from typing import Any, Dict, List, Tuple


def validate_sensor_data(data: Any, expected_features: int = 561) -> Tuple[bool, str]:
    """
    Validate sensor data from device
    
    Args:
        data: Input data to validate
        expected_features: Expected number of features
    
    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check if data exists
    if data is None:
        return False, "No data provided"
    
    # Check if data is list or array
    if not isinstance(data, (list, np.ndarray)):
        return False, "Data must be a list or array"
    
    # Convert to numpy array for validation with explicit float dtype
    try:
        # Force conversion to float to avoid object arrays
        data_array = np.array(data, dtype=np.float64)
    except (ValueError, TypeError) as e:
        return False, f"Cannot convert data to numeric array: {str(e)}"
    except Exception as e:
        return False, f"Cannot convert data to array: {str(e)}"
    
    # Check shape
    if len(data_array.shape) == 1:
        if len(data_array) != expected_features:
            return False, f"Expected {expected_features} features, got {len(data_array)}"
    elif len(data_array.shape) == 2:
        if data_array.shape[1] != expected_features:
            return False, f"Expected {expected_features} features, got {data_array.shape[1]}"
    else:
        return False, f"Invalid data shape: {data_array.shape}"
    
    # Check for invalid values (NaN, Inf) with safe operations
    try:
        nan_mask = np.isnan(data_array)
        if np.any(nan_mask):
            nan_count = np.sum(nan_mask)
            return False, f"Data contains {nan_count} NaN value(s)"
        
        inf_mask = np.isinf(data_array)
        if np.any(inf_mask):
            inf_count = np.sum(inf_mask)
            return False, f"Data contains {inf_count} infinite value(s)"
    except Exception as e:
        return False, f"Error checking data validity: {str(e)}"
    
    return True, ""


def validate_model_weights(weights: Any) -> Tuple[bool, str]:
    """
    Validate model weights for federated learning update
    
    Args:
        weights: Model weights to validate
    
    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check if weights exist
    if weights is None:
        return False, "No weights provided"
    
    # Check if weights is a list
    if not isinstance(weights, list):
        return False, "Weights must be a list of arrays"
    
    # Check if list is not empty
    if len(weights) == 0:
        return False, "Weights list is empty"
    
    # Validate each weight array
    for i, weight in enumerate(weights):
        try:
            weight_array = np.array(weight)
            
            # Check for invalid values
            if np.any(np.isnan(weight_array)):
                return False, f"Weight array {i} contains NaN values"
            
            if np.any(np.isinf(weight_array)):
                return False, f"Weight array {i} contains infinite values"
            
        except Exception as e:
            return False, f"Cannot convert weight {i} to array: {str(e)}"
    
    return True, ""


def validate_request_data(request_json: Dict, required_fields: List[str]) -> Tuple[bool, str]:
    """
    Validate that request contains all required fields
    
    Args:
        request_json: JSON request data
        required_fields: List of required field names
    
    Returns:
        Tuple of (is_valid, error_message)
    """
    if not request_json:
        return False, "Request body is empty"
    
    missing_fields = []
    for field in required_fields:
        if field not in request_json:
            missing_fields.append(field)
    
    if missing_fields:
        return False, f"Missing required fields: {', '.join(missing_fields)}"
    
    return True, ""

