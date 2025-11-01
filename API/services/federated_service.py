"""
Federated Learning service - handles FL model operations
"""
import numpy as np
import os
from typing import Dict, Any, Tuple, List
from utils.validators import validate_model_weights
from utils.metrics import MetricsTracker


class FederatedService:
    """Service for Federated Learning model operations"""
    
    def __init__(self, model_manager, config):
        self.model_manager = model_manager
        self.config = config
    
    def update_model_weights(self, client_training_data: dict) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        """
        Update federated learning model with REAL training from client data
        
        This implements TRUE federated learning:
        1. Client sends sensor data + true label
        2. Server performs local training on FL model (simulating on-device training)
        3. Server extracts REAL weight updates from training
        4. Server applies federated averaging with real weight deltas
        
        Args:
            client_training_data: Dict with 'sensor_data' and 'true_label'
        
        Returns:
            Tuple of (update_result, api_metrics)
        """
        # Initialize metrics tracker
        from utils.metrics import MetricsTracker
        from utils.preprocessing import preprocess_data
        from tensorflow.keras.utils import to_categorical
        import tensorflow as tf
        
        tracker = MetricsTracker()
        tracker.start()
        
        try:
            # Validate input
            if 'sensor_data' not in client_training_data:
                raise ValueError("Missing 'sensor_data' in training data")
            if 'true_label' not in client_training_data:
                raise ValueError("Missing 'true_label' in training data")
            
            # Get current model
            model = self.model_manager.get_fl_model()
            if model is None:
                raise ValueError("Federated Learning model not loaded")
            
            # Get weights BEFORE training
            weights_before = [w.copy() for w in self.model_manager.get_fl_model_weights()]
            
            # Preprocess client data
            sensor_data = np.array(client_training_data['sensor_data'], dtype=np.float32)
            processed_data = preprocess_data(sensor_data)
            
            # Prepare label (convert to one-hot)
            true_label = int(client_training_data['true_label'])
            if true_label < 0 or true_label >= self.config['NUM_CLASSES']:
                raise ValueError(f"Invalid label: {true_label}. Must be 0-{self.config['NUM_CLASSES']-1}")
            
            y_true = to_categorical([true_label], self.config['NUM_CLASSES'])
            
            # Perform REAL local training (simulating on-device training)
            # This is what would happen on the client device
            print(f"Performing local FL training with label: {self.config['ACTIVITY_LABELS'][true_label]}")
            
            # 🔥 CRITICAL FIX: Use EXTREMELY low learning rate to prevent catastrophic forgetting
            # When training on single samples, even 0.00001 causes accuracy degradation
            # Solution: Use 0.000001 (1,000x smaller than default!) for stable single-sample updates
            from tensorflow.keras.optimizers import Adam
            
            # Compile with ultra-conservative FL learning rate
            model.compile(
                optimizer=Adam(learning_rate=0.000001),  # 1000x smaller than default!
                loss='categorical_crossentropy',
                metrics=['accuracy']
            )
            
            # Train for a few steps (local training)
            # Single epoch with tiny LR allows learning without forgetting
            history = model.fit(
                processed_data,
                y_true,
                epochs=1,
                batch_size=1,
                verbose=0  # Silent training
            )
            
            # Get weights AFTER training
            weights_after = self.model_manager.get_fl_model_weights()
            
            # Calculate REAL weight deltas
            weight_deltas = []
            total_delta_magnitude = 0
            for w_before, w_after in zip(weights_before, weights_after):
                delta = w_after - w_before
                weight_deltas.append(delta)
                total_delta_magnitude += np.sum(np.abs(delta))
            
            # Apply federated averaging (for single client, this is just the update)
            # In real FL with multiple clients, you'd average deltas from many clients
            # For thesis: we apply the single client's update
            # The model was already updated by model.fit(), so weights are already new
            
            print(f"✅ FL update complete: loss={history.history['loss'][-1]:.4f}, delta_magnitude={total_delta_magnitude:.6f}")
            
            # Save updated model
            self.model_manager.save_fl_model()
            
            # Get updated model size
            model_size_kb = os.path.getsize(self.config['FL_MODEL_PATH']) / 1024
            
            # Stop metrics tracking
            api_metrics = tracker.stop()
            
            # Prepare result
            result = {
                'success': True,
                'message': 'Federated model updated with REAL training (ultra-conservative LR)',
                'model_size_kb': round(model_size_kb, 2),
                'num_weight_layers': len(weights_after),
                'training_loss': float(history.history['loss'][-1]),
                'weight_delta_magnitude': float(total_delta_magnitude),
                'local_epochs': 1,
                'learning_rate': 0.000001,  # Ultra-conservative FL LR (1000x smaller!)
                'trained_on_activity': self.config['ACTIVITY_LABELS'][true_label],
                'note': 'H5 trained with LR=0.000001 for maximum stability on single samples.'
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
                'error': str(e)
            }
            
            return result, api_metrics
    
    def get_model_for_device(self) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        """
        Get federated model information (without full weights - too large for mobile)
        
        Returns:
            Tuple of (model_info, api_metrics)
        """
        # Initialize metrics tracker
        from utils.metrics import MetricsTracker
        tracker = MetricsTracker()
        tracker.start()
        
        try:
            # Get model
            model = self.model_manager.get_fl_model()
            if model is None:
                raise ValueError("Federated Learning model not loaded")
            
            # Get model info (metadata only, no full weights)
            model_info = self.model_manager.get_model_info('fl')
            
            # Instead of sending full weights (too large!), send model metadata
            # The mobile app will use a lightweight simulator
            
            # Stop metrics tracking
            api_metrics = tracker.stop()
            
            # Prepare result - NO WEIGHTS, just metadata
            result = {
                'success': True,
                'model_info': model_info,
                'message': 'Federated model info retrieved (using lightweight mobile simulator)',
                'note': 'Full model weights not sent due to size - using local simulation'
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
                'error': str(e)
            }
            
            return result, api_metrics
    
    def get_model_info(self) -> Dict[str, Any]:
        """
        Get federated model information without weights
        
        Returns:
            Dictionary with model information
        """
        try:
            model_info = self.model_manager.get_model_info('fl')
            
            return {
                'success': True,
                'model_info': model_info
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }
    
    def update_model_with_deltas(self, delta_data: dict) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        """
        ✅ TRUE PRIVACY-PRESERVING FEDERATED LEARNING
        Update global model with weight deltas from client (NO RAW DATA!)
        
        This implements TRUE federated learning:
        1. Client trains locally and calculates weight deltas
        2. Client sends ONLY deltas (not raw data)
        3. Server applies federated averaging with deltas
        4. Global model updated without ever seeing raw data
        
        Args:
            delta_data: Dict with 'weight_deltas' and optional metadata
        
        Returns:
            Tuple of (update_result, api_metrics)
        """
        from utils.metrics import MetricsTracker
        
        tracker = MetricsTracker()
        tracker.start()
        
        try:
            # Validate input
            if 'weight_deltas' not in delta_data:
                raise ValueError("Missing 'weight_deltas' in request")
            
            # Get current model
            model = self.model_manager.get_fl_model()
            if model is None:
                raise ValueError("Federated Learning model not loaded")
            
            # Get current weights
            current_weights = model.get_weights()
            
            # Get weight deltas from client
            weight_deltas = delta_data['weight_deltas']
            
            # If no deltas provided (fallback case), use the legacy approach
            if not weight_deltas or len(weight_deltas) == 0:
                print("⚠️  No weight deltas provided, model not updated")
                print("This is expected if client-side training is not yet fully implemented")
                
                # Stop metrics tracking
                api_metrics = tracker.stop()
                
                return {
                    'success': True,
                    'message': 'No weight deltas to apply (client training pending)',
                    'delta_magnitude_received': 0.0,
                    'global_update_applied': False,
                    'note': 'Waiting for client-side PyTorch training implementation'
                }, api_metrics
            
            # Apply federated averaging
            # In real FL with multiple clients, you would:
            # 1. Collect deltas from multiple clients
            # 2. Average the deltas
            # 3. Apply averaged delta to global model
            # For single client (thesis demo), we apply the delta directly
            
            learning_rate = 1.0  # Can be adjusted for federated averaging
            num_samples = delta_data.get('num_samples', 1)
            
            # Calculate delta magnitude
            delta_magnitude = 0.0
            for delta in weight_deltas:
                delta_magnitude += np.sum(np.abs(delta))
            
            print(f"✅ Received weight deltas from client (magnitude: {delta_magnitude:.6f})")
            print(f"   Training samples: {num_samples}")
            print(f"   Activity: {delta_data.get('activity_name', 'Unknown')}")
            
            # Apply deltas to current weights
            new_weights = []
            for i, (current_w, delta) in enumerate(zip(current_weights, weight_deltas)):
                # new_weight = current_weight + learning_rate * delta
                new_w = current_w + learning_rate * np.array(delta)
                new_weights.append(new_w)
            
            # Update model with new weights
            model.set_weights(new_weights)
            
            # Recompile model
            model.compile(
                optimizer='adam',
                loss='categorical_crossentropy',
                metrics=['accuracy']
            )
            
            # Save updated model
            self.model_manager.save_fl_model()
            
            # Get updated model size
            model_size_kb = os.path.getsize(self.config['FL_MODEL_PATH']) / 1024
            
            # Stop metrics tracking
            api_metrics = tracker.stop()
            
            print(f"✅ Global FL model updated successfully!")
            print(f"   Privacy: Raw data never received")
            print(f"   Update: Only weight deltas applied")
            
            # Prepare result
            result = {
                'success': True,
                'message': 'Federated model updated with weight deltas (privacy-preserving)',
                'model_size_kb': round(model_size_kb, 2),
                'num_weight_layers': len(new_weights),
                'delta_magnitude_received': float(delta_magnitude),
                'global_update_applied': True,
                'training_loss_reported': delta_data.get('training_loss', 0.0),
                'activity_name': delta_data.get('activity_name', 'Unknown'),
                'note': '✅ TRUE FL: Only weight deltas received, raw data never sent'
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
                'error': str(e)
            }
            
            return result, api_metrics
    
    def test_model(self) -> Dict[str, Any]:
        """
        Test federated learning model health
        
        Returns:
            Dictionary with test results
        """
        try:
            model = self.model_manager.get_fl_model()
            
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
            
            # Get model info
            model_info = self.model_manager.get_model_info('fl')
            
            return {
                'success': True,
                'message': 'Federated Learning model is healthy',
                'model_info': model_info
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }

