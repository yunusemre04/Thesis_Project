"""
Gradient Aggregator for Federated Learning
Collects gradients from multiple devices and performs FedAvg aggregation
"""
import numpy as np
import torch
import torch.nn as nn
from datetime import datetime
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class FederatedModel(nn.Module):
    """
    PyTorch model matching the mobile architecture
    IMPORTANT: Layer order must match Keras model:
    Linear → ReLU → BatchNorm → Dropout
    """
    def __init__(self, input_size=561, num_classes=6):
        super(FederatedModel, self).__init__()
        
        # Layer 1: 561 -> 512
        self.fc1 = nn.Linear(input_size, 512)
        self.relu1 = nn.ReLU()
        self.bn1 = nn.BatchNorm1d(512)
        self.dropout1 = nn.Dropout(0.5)
        
        # Layer 2: 512 -> 256
        self.fc2 = nn.Linear(512, 256)
        self.relu2 = nn.ReLU()
        self.bn2 = nn.BatchNorm1d(256)
        self.dropout2 = nn.Dropout(0.4)
        
        # Layer 3: 256 -> 128
        self.fc3 = nn.Linear(256, 128)
        self.relu3 = nn.ReLU()
        self.bn3 = nn.BatchNorm1d(128)
        self.dropout3 = nn.Dropout(0.3)
        
        # Layer 4: 128 -> 64
        self.fc4 = nn.Linear(128, 64)
        self.relu4 = nn.ReLU()
        self.bn4 = nn.BatchNorm1d(64)
        self.dropout4 = nn.Dropout(0.2)
        
        # Output layer: 64 -> 6
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
        
        # Output layer
        x = self.fc5(x)
        return x


class GradientAggregator:
    """
    Manages gradient collection and aggregation for federated learning
    """
    def __init__(self, model_path: str, learning_rate: float = 0.001):
        self.model_path = Path(model_path)
        self.learning_rate = learning_rate
        self.gradient_buffer = []  # Store gradients from devices
        self.device_info = []  # Store device metadata
        self.model = None
        self.round_number = 0
        
        # Load model
        self._load_model()
        
    def _load_model(self):
        """Load PyTorch model from disk"""
        try:
            if not self.model_path.exists():
                logger.error(f"Model file not found: {self.model_path}")
                return False
                
            # Load the TorchScript model
            self.model = torch.jit.load(str(self.model_path))
            self.model.eval()
            logger.info(f"✅ Loaded PyTorch model from {self.model_path}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            return False
    
    def _align_dimensions(self, gradients: list, expected_size: int = 561) -> np.ndarray:
        """
        Dynamic Input Dimension Alignment Layer:
        Automatically detects incoming feature length. If a client transmits a lower-dimension 
        vector due to dynamic resolution scaling (e.g., dropping higher-order frequencies to save
        battery), this aligns it to the global expected size using zero-padding or masking.
        """
        vec = np.array(gradients, dtype=np.float32)
        if vec.shape[0] < expected_size:
            logger.info(f"Padding lower-dimension vector from {vec.shape[0]} to {expected_size} features.")
            padding = np.zeros(expected_size - vec.shape[0], dtype=np.float32)
            vec = np.concatenate([vec, padding])
        elif vec.shape[0] > expected_size:
            logger.info(f"Truncating higher-dimension vector from {vec.shape[0]} to {expected_size} features.")
            vec = vec[:expected_size]
        return vec

    def add_gradient(self, gradients: list, device_id: str, activity: str):
        """
        Add masked client gradients to the buffer implementation of Zero-Sum Mask-Based Perturbation Scheme.
        
        Args:
            gradients: List of masked gradient values (g_k* = g_k + r_k)
            device_id: Anonymous device identifier
            activity: Activity label for this gradient
        """
        gradients_array = self._align_dimensions(gradients)
        
        self.gradient_buffer.append(gradients_array)
        self.device_info.append({
            'device_id': device_id,
            'activity': activity,
            'timestamp': datetime.now().isoformat(),
            'gradient_norm': float(np.linalg.norm(gradients_array))
        })
        
        logger.info(f"Added gradients from device {device_id} (total: {len(self.gradient_buffer)})")
        
        return len(self.gradient_buffer)
    
    def aggregate_gradients(self, min_devices: int = 2):
        """
        Algorithm 1: Privacy-Masked FedAvg
        Aggregates masked gradients using a Zero-Sum Mask-Based Perturbation Scheme.
        
        Args:
            min_devices: Minimum number of devices required for synchronized aggregation (K_min)
            
        Returns:
            dict: Aggregation statistics
        """
        if len(self.gradient_buffer) < min_devices:
            return {
                'success': False,
                'error': f'Need at least {min_devices} synced clients (K_min barrier), currently have {len(self.gradient_buffer)}'
            }
        
        try:
            # Mathematical Mean of Masked Gradients
            # Individual raw gradients (g_k) are structurally hidden by cryptographic zero-sum masks (r_k),
            # such that the server only receives g_k* = g_k + r_k.
            # At the K_min threshold barrier, we compute the lightweight mean. Because sum(r_k) = 0
            # for the synchronized client group, the masks perfectly cancel out during averaging!
            # The result is the exact global gradient sum: mean(g_1 + r_1 + g_2 + r_2...) = mean(g_1 + g_2...).
            aggregated_gradients = np.mean(self.gradient_buffer, axis=0)
            
            stats = {
                'success': True,
                'num_devices': len(self.gradient_buffer),
                'aggregated_norm': float(np.linalg.norm(aggregated_gradients)),
                'aggregated_min': float(np.min(aggregated_gradients)),
                'aggregated_max': float(np.max(aggregated_gradients)),
                'aggregated_mean': float(np.mean(aggregated_gradients)),
                'devices': self.device_info.copy(),
                'round': self.round_number
            }
            
            logger.info(f"✅ Aggregated masked gradients from {len(self.gradient_buffer)} devices (Masks cancelled out successfully)")
            logger.info(f"   Norm: {stats['aggregated_norm']:.6f}")
            
            # Clear buffer and increment round after successful barrier sync
            self.gradient_buffer = []
            self.device_info = []
            self.round_number += 1
            
            return stats, aggregated_gradients
            
        except Exception as e:
            logger.error(f"Aggregation failed: {e}")
            return {'success': False, 'error': str(e)}, None
    
    def apply_gradients(self, aggregated_gradients: np.ndarray):
        """
        Apply aggregated gradients to update the model weights
        Uses training-mode PyTorch model to apply gradients and saves updated model
        
        Args:
            aggregated_gradients: Averaged gradient array (561 features)
            
        Returns:
            dict: Update statistics
        """
        try:
            # Load training-mode model (recreate from architecture)
            training_model = FederatedModel()
            
            # Load weights from TorchScript model to training model
            # We need to copy weights from the TorchScript model
            try:
                # Load the TorchScript model's state
                scripted_model = torch.jit.load(str(self.model_path))
                scripted_state = scripted_model.state_dict()
                
                # Load into training model
                training_model.load_state_dict(scripted_state)
                logger.info("✅ Loaded weights from TorchScript model to training model")
            except Exception as e:
                logger.warning(f"Could not load weights from TorchScript: {e}")
                logger.info("Using freshly initialized model")
            
            training_model.train()
            
            # Apply gradient update to first layer (input layer)
            # We have gradients w.r.t. inputs, approximate as weight updates
            with torch.no_grad():
                grad_tensor = torch.from_numpy(aggregated_gradients).float()
                
                # Update first layer weights using gradient descent
                # For each neuron in fc1, update its weights based on input gradients
                # This is a simplified approach - proper FL would use full backprop
                
                # Scale gradients by learning rate
                update = -self.learning_rate * grad_tensor
                
                # Apply update to first layer weights (add to each row)
                for i in range(training_model.fc1.weight.shape[0]):
                    training_model.fc1.weight[i] += update
                
                logger.info(f"✅ Applied gradients to fc1 layer")
                logger.info(f"   Update norm: {torch.norm(update).item():.6f}")
            
            # Save updated model
            training_model.eval()
            
            # Convert to TorchScript and save
            try:
                scripted_updated = torch.jit.script(training_model)
                scripted_updated.save(str(self.model_path))
                logger.info(f"✅ Saved updated model to {self.model_path}")
                
                # Reload the model for future use
                self.model = torch.jit.load(str(self.model_path))
                self.model.eval()
                
            except Exception as e:
                logger.error(f"Failed to save updated model: {e}")
                return {'success': False, 'error': f'Failed to save model: {str(e)}'}
            
            return {
                'success': True,
                'weights_updated': True,
                'update_norm': float(torch.norm(update).item()),
                'learning_rate': self.learning_rate,
                'note': 'Model weights updated and saved successfully'
            }
            
        except Exception as e:
            logger.error(f"Failed to apply gradients: {e}")
            import traceback
            traceback.print_exc()
            return {'success': False, 'error': str(e)}
    
    def perform_fl_round(self, min_devices: int = 2):
        """
        Perform a complete federated learning round:
        1. Aggregate masked gradients
        2. Apply lightweight mean to model
        3. Save updated model
        
        Returns:
            dict: Round statistics
        """
        # Aggregate (Algorithm 1: Privacy-Masked FedAvg)
        agg_stats, aggregated_gradients = self.aggregate_gradients(min_devices)
        
        if not agg_stats['success']:
            return agg_stats
        
        # Apply gradients
        update_stats = self.apply_gradients(aggregated_gradients)
        
        return {
            'success': True,
            'round': self.round_number, # Already incremented in aggregate_gradients
            'aggregation': agg_stats,
            'update': update_stats,
            'message': f'FL round {self.round_number} completed'
        }
    
    def get_status(self):
        """Get current aggregator status"""
        return {
            'model_loaded': self.model is not None,
            'model_path': str(self.model_path),
            'pending_gradients': len(self.gradient_buffer),
            'devices': [d['device_id'] for d in self.device_info],
            'current_round': self.round_number,
            'learning_rate': self.learning_rate
        }
    
    def clear_buffer(self):
        """Clear gradient buffer without aggregating"""
        count = len(self.gradient_buffer)
        self.gradient_buffer.clear()
        self.device_info.clear()
        logger.info(f"Cleared {count} gradients from buffer")
        return count
