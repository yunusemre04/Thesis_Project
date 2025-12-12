"""
Deep Learning service - handles DL model operations
"""
import numpy as np
from typing import Dict, Any, Tuple
from utils.preprocessing import preprocess_data, validate_input_shape
from utils.validators import validate_sensor_data
from utils.metrics import MetricsTracker


class DeepLearningService:
    """Service for Deep Learning model operations"""
    
    def __init__(self, model_manager, config):
        self.model_manager = model_manager
        self.config = config
    
    def predict(self, sensor_data: list) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        """
        Perform prediction using deep learning model
        
        Args:
            sensor_data: List of 561 sensor features
        
        Returns:
            Tuple of (prediction_result, api_metrics)
        """
        # Initialize metrics tracker
        tracker = MetricsTracker()
        tracker.start()
        
        try:
            # Validate input data
            is_valid, error_msg = validate_sensor_data(sensor_data, self.config['NUM_FEATURES'])
            if not is_valid:
                raise ValueError(error_msg)
            
            # Log raw input statistics
            import numpy as np
            sensor_array = np.array(sensor_data)
            print(f"📥 DL Input: min={sensor_array.min():.4f}, max={sensor_array.max():.4f}, mean={sensor_array.mean():.4f}")
            print(f"   First 10 features: {sensor_array[:10]}")
            
            # Get scaler for preprocessing
            scaler = self.model_manager.get_dl_scaler()
            if scaler is None:
                print("⚠️  WARNING: DL Scaler is None!")
            else:
                print(f"✅ DL Scaler loaded (mean shape: {scaler.mean_.shape})")
            
            # Preprocess data with UCI HAR scaler
            processed_data = preprocess_data(sensor_data, scaler=scaler)
            print(f"📤 DL After preprocessing: min={processed_data.min():.4f}, max={processed_data.max():.4f}, mean={processed_data.mean():.4f}")
            
            # Validate shape
            if not validate_input_shape(processed_data, self.config['NUM_FEATURES']):
                raise ValueError(f"Invalid input shape. Expected {self.config['NUM_FEATURES']} features")
            
            # Get model
            model = self.model_manager.get_dl_model()
            if model is None:
                raise ValueError("Deep Learning model not loaded")
            
            # Perform prediction
            predictions = model.predict(processed_data, verbose=0)
            
            # Get predicted class and confidence
            predicted_class_idx = int(np.argmax(predictions[0]))
            confidence = float(predictions[0][predicted_class_idx])
            predicted_activity = self.config['ACTIVITY_LABELS'][predicted_class_idx]
            
            # Get all class probabilities (convert to percentage for display)
            all_probabilities = {
                self.config['ACTIVITY_LABELS'][i]: round(float(predictions[0][i]) * 100, 2)
                for i in range(len(self.config['ACTIVITY_LABELS']))
            }
            
            # Stop metrics tracking
            api_metrics = tracker.stop()
            
            # Prepare result
            result = {
                'success': True,
                'prediction': {
                    'activity': predicted_activity,
                    'class_index': predicted_class_idx,
                    'confidence': round(confidence * 100, 2),
                    'all_probabilities': all_probabilities
                },
                'model_type': 'Deep Learning'
            }
            
            return result, api_metrics
            
        except Exception as e:
            # Try to get metrics even on error
            try:
                api_metrics = tracker.stop()
            except:
                api_metrics = {
                    'cpu_usage_percent': 0.0,
                    'ram_usage_mb': 0.0,
                    'duration_seconds': 0.0
                }
            
            result = {
                'success': False,
                'error': str(e),
                'model_type': 'Deep Learning'
            }
            
            return result, api_metrics
    
    def test_model(self) -> Dict[str, Any]:
        """
        Test deep learning model health
        
        Returns:
            Dictionary with test results
        """
        try:
            model = self.model_manager.get_dl_model()
            
            if model is None:
                return {
                    'success': False,
                    'error': 'Model not loaded'
                }
            
            # Create dummy input for testing
            dummy_input = np.random.randn(1, self.config['NUM_FEATURES']).astype(np.float32)
            
            # Try prediction
            predictions = model.predict(dummy_input, verbose=0)
            
            # Verify output shape
            if predictions.shape[1] != self.config['NUM_CLASSES']:
                return {
                    'success': False,
                    'error': f'Invalid output shape: {predictions.shape}'
                }
            
            return {
                'success': True,
                'message': 'Deep Learning model is healthy',
                'model_info': self.model_manager.get_model_info('dl')
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }

