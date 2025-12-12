"""
Model loading and management service
"""
import os
import tensorflow as tf
from typing import Optional, Dict, Any
import numpy as np


class ModelManager:
    """Manage loading and caching of ML models"""
    
    def __init__(self, config):
        self.config = config
        self._dl_model = None
        self._fl_model = None
        self._dl_scaler = None  # NEW: StandardScaler for DL
        self._fl_scaler = None  # NEW: StandardScaler for FL
        self._models_loaded = False
    
    def load_models(self):
        """Load both deep learning and federated learning models"""
        try:
            import joblib
            print("Loading models...")
            
            # Load Deep Learning model
            if os.path.exists(self.config['DL_MODEL_PATH']):
                self._dl_model = tf.keras.models.load_model(self.config['DL_MODEL_PATH'])
                # Recompile to ensure optimizer works with current variables
                self._dl_model.compile(
                    optimizer='adam',
                    loss='categorical_crossentropy',
                    metrics=['accuracy']
                )
                print(f"✓ Deep Learning model loaded from {self.config['DL_MODEL_PATH']}")
            else:
                print(f"⚠ Deep Learning model not found at {self.config['DL_MODEL_PATH']}")
            
            # Load DL Scaler (UCI HAR training statistics)
            if os.path.exists(self.config['DL_SCALER_PATH']):
                self._dl_scaler = joblib.load(self.config['DL_SCALER_PATH'])
                print(f"✓ DL Scaler loaded from {self.config['DL_SCALER_PATH']}")
            else:
                print(f"⚠ DL Scaler not found at {self.config['DL_SCALER_PATH']}")
            
            # Load Federated Learning model
            if os.path.exists(self.config['FL_MODEL_PATH']):
                self._fl_model = tf.keras.models.load_model(self.config['FL_MODEL_PATH'])
                # Recompile to ensure optimizer works with current variables
                self._fl_model.compile(
                    optimizer='adam',
                    loss='categorical_crossentropy',
                    metrics=['accuracy']
                )
                print(f"✓ Federated Learning model loaded from {self.config['FL_MODEL_PATH']}")
            else:
                print(f"⚠ Federated Learning model not found at {self.config['FL_MODEL_PATH']}")
            
            # Load FL Scaler (UCI HAR training statistics)
            if os.path.exists(self.config['FL_SCALER_PATH']):
                self._fl_scaler = joblib.load(self.config['FL_SCALER_PATH'])
                print(f"✓ FL Scaler loaded from {self.config['FL_SCALER_PATH']}")
            else:
                print(f"⚠ FL Scaler not found at {self.config['FL_SCALER_PATH']}")
            
            self._models_loaded = True
            print("Models loaded successfully!")
            
        except Exception as e:
            print(f"✗ Error loading models: {str(e)}")
            raise
    
    def get_dl_model(self):
        """Get the deep learning model"""
        if not self._models_loaded:
            self.load_models()
        return self._dl_model
    
    def get_fl_model(self):
        """Get the federated learning model"""
        if not self._models_loaded:
            self.load_models()
        return self._fl_model
    
    def get_dl_scaler(self):
        """Get the deep learning scaler"""
        if not self._models_loaded:
            self.load_models()
        return self._dl_scaler
    
    def get_fl_scaler(self):
        """Get the federated learning scaler"""
        if not self._models_loaded:
            self.load_models()
        return self._fl_scaler
    
    def get_model_info(self, model_type: str) -> Dict[str, Any]:
        """
        Get information about a model
        
        Args:
            model_type: 'dl' or 'fl'
        
        Returns:
            Dictionary with model information
        """
        model = self._dl_model if model_type == 'dl' else self._fl_model
        
        if model is None:
            return {
                'loaded': False,
                'error': 'Model not loaded'
            }
        
        # Get model size
        model_path = self.config['DL_MODEL_PATH'] if model_type == 'dl' else self.config['FL_MODEL_PATH']
        model_size_bytes = os.path.getsize(model_path) if os.path.exists(model_path) else 0
        model_size_kb = model_size_bytes / 1024
        
        return {
            'loaded': True,
            'model_type': 'Deep Learning' if model_type == 'dl' else 'Federated Learning',
            'input_shape': model.input_shape[1:],
            'output_shape': model.output_shape[1:],
            'num_layers': len(model.layers),
            'num_parameters': model.count_params(),
            'model_size_kb': round(model_size_kb, 2),
            'activity_labels': self.config['ACTIVITY_LABELS']
        }
    
    def update_fl_model(self, new_weights: list):
        """
        Update federated learning model with new weights
        
        Args:
            new_weights: List of weight arrays
        """
        if self._fl_model is None:
            raise ValueError("Federated model not loaded")
        
        # Convert weights to numpy arrays
        weights_arrays = [np.array(w) for w in new_weights]
        
        # Update model weights
        self._fl_model.set_weights(weights_arrays)
        
        # Save updated model
        self._fl_model.save(self.config['FL_MODEL_PATH'])
    
    def get_fl_model_weights(self) -> list:
        """
        Get current federated learning model weights
        
        Returns:
            List of weight arrays (as numpy arrays)
        """
        if self._fl_model is None:
            raise ValueError("Federated model not loaded")
        
        # Get weights and ensure they're numpy arrays
        weights = self._fl_model.get_weights()
        # Convert any tensors to numpy arrays
        import numpy as np
        return [np.array(w) if hasattr(w, 'numpy') else w for w in weights]
    
    def save_fl_model(self):
        """Save the federated learning model"""
        if self._fl_model is None:
            raise ValueError("Federated model not loaded")
        
        # Recompile before saving to ensure optimizer works with updated weights
        self._fl_model.compile(
            optimizer='adam',
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self._fl_model.save(self.config['FL_MODEL_PATH'])
        print(f"Federated model saved to {self.config['FL_MODEL_PATH']}")
    
    def reload_models(self):
        """Reload all models from disk"""
        self._models_loaded = False
        self._dl_model = None
        self._fl_model = None
        self.load_models()

