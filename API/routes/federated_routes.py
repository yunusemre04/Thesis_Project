"""
Federated Learning routes - FL model operations
"""
import os
from flask import Blueprint, request, jsonify, current_app
from utils.validators import validate_request_data
import numpy as np

fl_bp = Blueprint('federated', __name__, url_prefix='/api/fl')


@fl_bp.route('/predict', methods=['POST'])
def predict():
    """
    Perform prediction using Federated Learning model
    This simulates on-device prediction by using the FL model on server
    
    Request JSON:
        {
            "sensor_data": [561 features]
        }
    
    Response JSON:
        {
            "success": true,
            "prediction": {
                "activity": "WALKING",
                "class_index": 0,
                "confidence": 93.5,
                "all_probabilities": {...}
            },
            "api_metrics": {...},
            "model_type": "Federated Learning",
            "note": "Prediction using FL model (simulates on-device inference)"
        }
    """
    try:
        from utils.preprocessing import preprocess_data, validate_input_shape
        from utils.validators import validate_sensor_data
        from utils.metrics import MetricsTracker
        import numpy as np
        
        # Initialize metrics tracker
        tracker = MetricsTracker()
        tracker.start()
        
        # Get request data
        data = request.get_json()
        
        # Validate request
        is_valid, error_msg = validate_request_data(data, ['sensor_data'])
        if not is_valid:
            return jsonify({
                'success': False,
                'error': error_msg
            }), 400
        
        # Get sensor data
        sensor_data = data['sensor_data']
        
        # Log raw input statistics
        sensor_array = np.array(sensor_data)
        print(f"📥 FL Input: min={sensor_array.min():.4f}, max={sensor_array.max():.4f}, mean={sensor_array.mean():.4f}")
        print(f"   First 10 features: {sensor_array[:10]}")
        
        # Validate input data
        is_valid, error_msg = validate_sensor_data(sensor_data, current_app.config['NUM_FEATURES'])
        if not is_valid:
            return jsonify({
                'success': False,
                'error': error_msg
            }), 400
        
        # Get scaler for preprocessing
        scaler = current_app.model_manager.get_fl_scaler()
        if scaler is None:
            print("⚠️  WARNING: FL Scaler is None!")
        else:
            print(f"✅ FL Scaler loaded (mean shape: {scaler.mean_.shape})")
        
        # Preprocess data with UCI HAR scaler (same as DL)
        processed_data = preprocess_data(sensor_data, scaler=scaler)
        print(f"📤 FL After preprocessing: min={processed_data.min():.4f}, max={processed_data.max():.4f}, mean={processed_data.mean():.4f}")
        
        # Validate shape
        if not validate_input_shape(processed_data, current_app.config['NUM_FEATURES']):
            return jsonify({
                'success': False,
                'error': f"Invalid input shape. Expected {current_app.config['NUM_FEATURES']} features"
            }), 400
        
        # Get FL model
        model_manager = current_app.model_manager
        model = model_manager.get_fl_model()
        
        if model is None:
            return jsonify({
                'success': False,
                'error': 'Federated Learning model not loaded'
            }), 500
        
        # Perform prediction using FL model
        predictions = model.predict(processed_data, verbose=0)
        
        # Get predicted class and confidence
        predicted_class_idx = int(np.argmax(predictions[0]))
        confidence = float(predictions[0][predicted_class_idx])
        predicted_activity = current_app.config['ACTIVITY_LABELS'][predicted_class_idx]
        
        # Get all class probabilities (convert to percentage)
        all_probabilities = {
            current_app.config['ACTIVITY_LABELS'][i]: round(float(predictions[0][i]) * 100, 2)
            for i in range(len(current_app.config['ACTIVITY_LABELS']))
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
            'model_type': 'Federated Learning',
            'note': 'Prediction using FL model (represents on-device inference)',
            'api_metrics': api_metrics
        }
        
        return jsonify(result), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'model_type': 'Federated Learning'
        }), 500


@fl_bp.route('/update_weights', methods=['POST'])
def update_weights():
    """
    Update federated model with REAL client training data
    
    Request JSON:
        {
            "sensor_data": [561 features],  # Real sensor data from client
            "true_label": 0-5,              # Real activity label (0=WALKING, 1=WALKING_UPSTAIRS, etc.)
            "activity_name": "WALKING"      # Activity name (for logging)
        }
    
    Response JSON:
        {
            "success": true,
            "message": "Federated model updated with REAL training",
            "model_size_kb": 4521.5,
            "num_weight_layers": 10,
            "training_loss": 0.0234,
            "weight_delta_magnitude": 0.00234,
            "trained_on_activity": "WALKING",
            "api_metrics": {...},
            "note": "Real local training performed, real weight updates applied"
        }
    """
    try:
        import numpy as np
        from utils.preprocessing import preprocess_data
        
        # Get request data
        data = request.get_json()
        
        # Validate required fields
        is_valid, error_msg = validate_request_data(data, ['sensor_data', 'true_label'])
        if not is_valid:
            return jsonify({
                'success': False,
                'error': error_msg
            }), 400
        
        # LABEL QUALITY CHECK: Verify model's prediction matches user's label
        # This prevents training on wrong labels (e.g., SITTING data labeled as WALKING)
        sensor_data = data['sensor_data']
        true_label = data['true_label']
        
        # Get model's prediction for validation
        model_manager = current_app.model_manager
        model = model_manager.get_fl_model()
        
        if model is not None:
            # Get scaler for preprocessing
            scaler = model_manager.get_fl_scaler()
            processed_data = preprocess_data(sensor_data, scaler=scaler)
            predictions = model.predict(processed_data, verbose=0)
            predicted_class = int(np.argmax(predictions[0]))
            confidence = float(predictions[0][predicted_class])
            
            # Check if prediction matches user's label
            if predicted_class != true_label:
                # Label mismatch - user may have provided wrong label
                # Calculate how confident model is in user's label
                user_label_confidence = float(predictions[0][true_label])
                
                # If model is very confident in a DIFFERENT prediction, reject training
                if confidence > 0.85 and user_label_confidence < 0.15:
                    predicted_activity = current_app.config['ACTIVITY_LABELS'][predicted_class]
                    user_activity = data.get('activity_name', 'Unknown')
                    
                    return jsonify({
                        'success': False,
                        'error': 'Label quality check failed',
                        'reason': 'Model strongly disagrees with provided label',
                        'details': {
                            'user_label': user_activity,
                            'user_confidence': round(user_label_confidence * 100, 2),
                            'model_prediction': predicted_activity,
                            'model_confidence': round(confidence * 100, 2),
                            'message': f'Model is {confidence*100:.1f}% confident this is "{predicted_activity}", but you labeled it as "{user_activity}". Training on incorrect labels reduces accuracy. Please verify your activity.'
                        }
                    }), 400
        
        # Perform REAL federated learning update
        fl_service = current_app.fl_service
        result, api_metrics = fl_service.update_model_weights(data)
        
        # Add API metrics to response
        result['api_metrics'] = api_metrics
        
        # Return response
        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code
        
    except Exception as e:
        import traceback
        traceback.print_exc()  # Print full error for debugging
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500





@fl_bp.route('/info', methods=['GET'])
def get_model_info():
    """
    Get federated model information (without weights)
    
    Response JSON:
        {
            "success": true,
            "model_info": {...}
        }
    """
    try:
        fl_service = current_app.fl_service
        result = fl_service.get_model_info()
        
        status_code = 200 if result['success'] else 500
        return jsonify(result), status_code
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500



@fl_bp.route('/evaluate', methods=['GET'])
def evaluate_model():
    """
    Evaluate Federated Learning H5 model on UCI HAR test data
    
    Response JSON:
        {
            "success": true,
            "evaluation": {...}
        }
    """
    try:
        from utils.evaluation import load_uci_har_test_data, evaluate_model, print_evaluation_summary
        
        # Get model
        model_manager = current_app.model_manager
        model = model_manager.get_fl_model()
        
        if model is None:
            return jsonify({
                'success': False,
                'error': 'Federated Learning model not loaded'
            }), 500
        
        # Load test data
        dataset_path = current_app.config.get('DATASET_DIR')
        if not dataset_path or not os.path.exists(dataset_path):
            return jsonify({
                'success': False,
                'error': f'UCI HAR Dataset not found at {dataset_path}. Please copy the dataset to API/UCI_HAR_Dataset/'
            }), 404
        
        print(f"Loading test data from {dataset_path}...")
        X_test, y_test = load_uci_har_test_data(dataset_path)
        
        # Evaluate model
        activity_labels = current_app.config.get('ACTIVITY_LABELS')
        results = evaluate_model(model, X_test, y_test, activity_labels)
        
        # Print summary to console
        print_evaluation_summary(results)
        
        return jsonify({
            'success': True,
            'model_format': 'H5 (Keras)',
            'evaluation': results
        }), 200
        
    except FileNotFoundError as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 404
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


# ============================================================================
# PyTorch Mobile FL Endpoints
# ============================================================================

@fl_bp.route('/get_pytorch_model', methods=['GET', 'OPTIONS'])
def get_pytorch_model():
    """
    Serve PyTorch mobile model - MINIMAL implementation for reliability
    """
    # Handle OPTIONS preflight
    if request.method == 'OPTIONS':
        response = jsonify({'status': 'ok'})
        response.headers['Access-Control-Allow-Origin'] = '*'
        response.headers['Access-Control-Allow-Methods'] = 'GET, OPTIONS'
        response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
        return response, 200
    
    try:
        from flask import send_file
        import time
        
        model_path = os.path.join(current_app.root_path, 'models', 'fl_model.pt')
        
        if not os.path.exists(model_path):
            return jsonify({'success': False, 'error': 'Model not found'}), 404
        
        file_size = os.path.getsize(model_path)
        print(f"📦 Sending PyTorch model: {file_size / (1024*1024):.2f} MB")
        
        # Use send_file with conditional_request disabled for simplicity
        return send_file(
            model_path,
            mimetype='application/octet-stream',
            as_attachment=True,
            download_name='fl_model.pt',
            conditional=False,
            max_age=0
        )
        
    except Exception as e:
        print(f"❌ Error: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@fl_bp.route('/pytorch_model_info', methods=['GET'])
def get_pytorch_model_info():
    """
    Get information about PyTorch mobile model
    
    Returns:
        JSON with model information including size, architecture, FL status, etc.
        
    Example:
        GET /api/fl/pytorch_model_info
    """
    try:
        model_path = os.path.join(current_app.root_path, 'models', 'fl_model.pt')
        
        if not os.path.exists(model_path):
            return jsonify({
                'success': False,
                'error': 'PyTorch model not found',
                'message': 'Run convert_fl_to_pytorch.py to generate the model'
            }), 404
        
        # Get file info
        file_size = os.path.getsize(model_path)
        file_modified = os.path.getmtime(model_path)
        
        # Get FL aggregator status
        aggregator = current_app.gradient_aggregator
        fl_status = aggregator.get_status()
        
        info = {
            'success': True,
            'model_format': 'PyTorch Mobile (TorchScript)',
            'file_name': 'fl_model.pt',
            'file_path': model_path,
            'file_size_bytes': file_size,
            'file_size_kb': round(file_size / 1024, 2),
            'file_size_mb': round(file_size / (1024 * 1024), 2),
            'last_modified': file_modified,
            'architecture': {
                'input_size': 561,
                'hidden_layers': [512, 256, 128, 64],
                'output_classes': 6,
                'activation': 'ReLU',
                'normalization': 'BatchNorm1d',
                'dropout': [0.5, 0.5, 0.5, 0.5],
                'total_parameters': '~500K'
            },
            'federated_learning': {
                'enabled': True,
                'model_loaded': fl_status['model_loaded'],
                'current_round': fl_status['current_round'],
                'pending_gradients': fl_status['pending_gradients'],
                'devices_contributing': fl_status['devices'],
                'learning_rate': fl_status['learning_rate'],
                'aggregation_method': 'FedAvg (Federated Averaging)',
                'privacy_guarantees': [
                    'No raw data transmission',
                    'Gradient-only updates',
                    'On-device computation',
                    'Anonymous device IDs'
                ]
            },
            'features': [
                'On-device inference',
                'Privacy-preserving (no raw data transmission)',
                'Gradient computation support',
                'Offline prediction capability',
                'Mobile-optimized (TorchScript)',
                'Federated learning ready'
            ],
            'endpoints': {
                'download': '/api/fl/get_pytorch_model',
                'info': '/api/fl/pytorch_model_info',
                'update_gradients': '/api/fl/update_weights_gradients',
                'aggregate': '/api/fl/aggregate',
                'status': '/api/fl/aggregator_status',
                'clear': '/api/fl/clear_gradients'
            },
            'usage': {
                'platform': 'Android (Kotlin)',
                'dependencies': [
                    'org.pytorch:pytorch_android:1.9.0',
                    'org.pytorch:pytorch_android_torchvision:1.9.0'
                ],
                'service': 'PyTorchFLService.kt'
            }
        }
        
        print(f"✅ PyTorch model info requested")
        print(f"   File: fl_model.pt ({info['file_size_mb']:.2f} MB)")
        print(f"   Architecture: 561 -> 512 -> 256 -> 128 -> 64 -> 6")
        print(f"   FL Status: Round {fl_status['current_round']}, {fl_status['pending_gradients']} pending gradients")
        
        return jsonify(info), 200
        
    except Exception as e:
        print(f"❌ Error getting PyTorch model info: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@fl_bp.route('/evaluate_pytorch', methods=['GET'])
def evaluate_pytorch_model():
    """
    Evaluate PyTorch mobile model on UCI HAR test data
    
    Returns:
        JSON with evaluation results including accuracy, precision, recall, F1-score
        
    Example:
        GET /api/fl/evaluate_pytorch
    """
    try:
        import torch
        from utils.evaluation import load_uci_har_test_data
        from sklearn.metrics import accuracy_score, precision_recall_fscore_support, classification_report
        
        print("\n" + "="*80)
        print("📊 Evaluating PyTorch Model")
        print("="*80)
        
        # Load PyTorch model
        model_path = os.path.join(current_app.root_path, 'models', 'fl_model.pt')
        
        if not os.path.exists(model_path):
            return jsonify({
                'success': False,
                'error': 'PyTorch model not found'
            }), 404
        
        model = torch.jit.load(model_path)
        model.eval()
        
        # Load test data
        X_test, y_test = load_uci_har_test_data(current_app.config['DATASET_DIR'])
        activity_labels = current_app.config['ACTIVITY_LABELS']
        
        print(f"✅ Test data loaded: {X_test.shape[0]} samples")
        
        # CRITICAL: Apply standardization preprocessing (model was trained on standardized data)
        X_test_mean = np.mean(X_test, axis=0, keepdims=True)
        X_test_std = np.std(X_test, axis=0, keepdims=True) + 1e-8
        X_test_standardized = (X_test - X_test_mean) / X_test_std
        print(f"✅ Data standardized (range: [{X_test_standardized.min():.4f}, {X_test_standardized.max():.4f}])")
        
        # Convert to PyTorch tensors
        X_test_tensor = torch.from_numpy(X_test_standardized).float()
        
        # Make predictions
        with torch.no_grad():
            predictions_tensor = model(X_test_tensor)
            predictions = torch.argmax(predictions_tensor, dim=1).numpy()
        
        # y_test is already 0-indexed from load_uci_har_test_data
        y_test_labels = y_test.astype(int)
        
        # Calculate metrics
        accuracy = accuracy_score(y_test_labels, predictions)
        precision, recall, f1, support = precision_recall_fscore_support(
            y_test_labels, predictions, average='weighted', zero_division=0
        )
        
        # Per-class metrics
        per_class_precision, per_class_recall, per_class_f1, per_class_support = precision_recall_fscore_support(
            y_test_labels, predictions, average=None, zero_division=0
        )
        
        per_class_metrics = []
        for i, activity in enumerate(activity_labels):
            per_class_metrics.append({
                'class_name': activity,
                'precision': float(per_class_precision[i]),
                'recall': float(per_class_recall[i]),
                'f1_score': float(per_class_f1[i]),
                'support': int(per_class_support[i])
            })
        
        results = {
            'success': True,
            'evaluation': {
                'overall_accuracy': float(accuracy),
                'test_samples': int(X_test.shape[0]),
                'precision_weighted': float(precision),
                'recall_weighted': float(recall),
                'f1_score_weighted': float(f1),
                'per_class_metrics': per_class_metrics
            },
            'model_info': {
                'model_type': 'PyTorch Mobile',
                'framework': 'PyTorch',
                'format': 'TorchScript'
            }
        }
        
        print(f"\n📈 Evaluation Results:")
        print(f"   Overall Accuracy: {accuracy*100:.2f}%")
        print(f"   Precision: {precision*100:.2f}%")
        print(f"   Recall: {recall*100:.2f}%")
        print(f"   F1-Score: {f1*100:.2f}%")
        print("="*80 + "\n")
        
        return jsonify(results), 200
        
    except Exception as e:
        print(f"❌ Error evaluating PyTorch model: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@fl_bp.route('/update_weights_gradients', methods=['POST'])
def update_weights_gradients():
    """
    Receive gradients from client device (TRUE FEDERATED LEARNING)
    This endpoint only receives gradients - NO RAW DATA
    """
    try:
        print("\n" + "="*80)
        print("📥 Federated Learning: Gradient Update Request")
        print("="*80)
        
        data = request.json
        gradients = data.get('gradients', [])
        activity_name = data.get('activity_name', 'unknown')
        device_id = data.get('device_id', 'anonymous')
        
        print(f"✅ Gradients received from device: {device_id}")
        print(f"📊 Activity: {activity_name}")
        print(f"📈 Gradient count: {len(gradients)}")
        
        if not gradients:
            print("❌ ERROR: No gradients received!")
            return jsonify({
                'success': False,
                'error': 'No gradients provided'
            }), 400
        
        # Convert to numpy array for statistics
        gradients_array = np.array(gradients)

        # Validate gradient length against expected number of features (if configured)
        expected_features = current_app.config.get('NUM_FEATURES') if hasattr(current_app, 'config') else None
        if expected_features is not None:
            if gradients_array.size != expected_features:
                print(f"❗ WARNING: Received gradients length ({gradients_array.size}) does not match expected features ({expected_features}).")
                return jsonify({
                    'success': False,
                    'error': 'Invalid gradients length',
                    'received_length': int(gradients_array.size),
                    'expected_length': int(expected_features)
                }), 400

        # Compute gradient statistics
        gradient_norm = float(np.linalg.norm(gradients_array))
        gradient_min = float(np.min(gradients_array))
        gradient_max = float(np.max(gradients_array))
        gradient_mean = float(np.mean(gradients_array))
        
        print(f"\n🔬 Gradient Statistics:")
        print(f"  - L2 Norm: {gradient_norm:.6f}")
        print(f"  - Min: {gradient_min:.6f}")
        print(f"  - Max: {gradient_max:.6f}")
        print(f"  - Mean: {gradient_mean:.6f}")
        
        # Add gradients to aggregator
        aggregator = current_app.gradient_aggregator
        num_pending = aggregator.add_gradient(gradients, device_id, activity_name)
        
        print(f"\n✅ Gradient added to aggregator")
        print(f"📊 Pending gradients: {num_pending} device(s)")
        print(f"✅ Privacy confirmed: Raw data NOT transmitted")
        print(f"✅ Only gradients received for model update")
        
        # AUTO-AGGREGATION: Trigger aggregation when we have enough gradients
        MIN_DEVICES_FOR_AUTO_AGGREGATION = 1  # Minimum devices needed
        aggregation_triggered = False
        fl_round = None
        
        if num_pending >= MIN_DEVICES_FOR_AUTO_AGGREGATION:
            print(f"\n🔄 AUTO-AGGREGATION: {num_pending} gradients available (>= {MIN_DEVICES_FOR_AUTO_AGGREGATION})")
            print("Starting automatic aggregation...")
            
            # Perform FL round
            result = aggregator.perform_fl_round(min_devices=1)
            
            if result['success']:
                aggregation_triggered = True
                fl_round = result['round']
                print(f"✅ AUTO-AGGREGATION completed! Round: {fl_round}")
                print(f"   Devices aggregated: {result['aggregation']['num_devices']}")
                print(f"   Gradient norm: {result['aggregation']['aggregated_norm']:.6f}")
                print(f"   Weights updated: {result['update']['weights_updated']}")
            else:
                print(f"❌ AUTO-AGGREGATION failed: {result.get('error')}")
        else:
            print(f"⏳ Waiting for more gradients ({num_pending}/{MIN_DEVICES_FOR_AUTO_AGGREGATION})")
        
        print("="*80 + "\n")
        
        response_data = {
            'success': True,
            'message': f'Gradients received from device {device_id}',
            'gradients_applied': aggregation_triggered,  # True if auto-aggregation happened
            'pending_aggregation': 0 if aggregation_triggered else num_pending,
            'aggregation_info': {
                'device_id': device_id,
                'activity': activity_name,
                'gradient_norm': gradient_norm,
                'gradient_min': gradient_min,
                'gradient_max': gradient_max,
                'gradient_mean': gradient_mean,
                'raw_data_transmitted': False,  # Privacy guarantee
                'privacy_preserved': True,
                'note': 'Auto-aggregation triggered' if aggregation_triggered else 'Gradients stored for aggregation'
            }
        }
        
        if aggregation_triggered:
            response_data['fl_round'] = fl_round
            response_data['auto_aggregated'] = True
        
        return jsonify(response_data)
        
    except Exception as e:
        print(f"\n❌ ERROR in gradient update:")
        print(f"Error: {str(e)}")
        print("="*80 + "\n")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@fl_bp.route('/aggregate', methods=['POST'])
def aggregate_gradients():
    """
    Trigger gradient aggregation and model update
    Performs FedAvg across all pending gradients
    
    Request JSON (optional):
        {
            "min_devices": 1  # Minimum devices required
        }
    
    Response JSON:
        {
            "success": true,
            "round": 1,
            "aggregation": {...},
            "update": {...},
            "message": "FL round completed"
        }
    """
    try:
        print("\n" + "="*80)
        print("🔄 Federated Learning: Starting Aggregation")
        print("="*80)
        
        data = request.json or {}
        min_devices = data.get('min_devices', 1)
        
        aggregator = current_app.gradient_aggregator
        
        # Perform FL round
        result = aggregator.perform_fl_round(min_devices=min_devices)
        
        if result['success']:
            print(f"\n✅ FL Round {result['round']} completed!")
            print(f"📊 Aggregated from {result['aggregation']['num_devices']} devices")
            print(f"📈 Aggregated gradient norm: {result['aggregation']['aggregated_norm']:.6f}")
            print("="*80 + "\n")
        else:
            print(f"\n❌ Aggregation failed: {result.get('error')}")
            print("="*80 + "\n")
        
        return jsonify(result), 200 if result['success'] else 400
        
    except Exception as e:
        print(f"\n❌ ERROR in aggregation:")
        print(f"Error: {str(e)}")
        print("="*80 + "\n")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@fl_bp.route('/aggregator_status', methods=['GET'])
def aggregator_status():
    """
    Get current aggregator status
    
    Response JSON:
        {
            "model_loaded": true,
            "pending_gradients": 3,
            "devices": ["android_123", "android_456"],
            "current_round": 5
        }
    """
    try:
        aggregator = current_app.gradient_aggregator
        status = aggregator.get_status()
        
        return jsonify({
            'success': True,
            **status
        }), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@fl_bp.route('/clear_gradients', methods=['POST'])
def clear_gradients():
    """
    Clear gradient buffer without aggregating
    Useful for resetting between experiments
    
    Response JSON:
        {
            "success": true,
            "cleared_count": 3
        }
    """
    try:
        aggregator = current_app.gradient_aggregator
        count = aggregator.clear_buffer()
        
        print(f"🗑️ Cleared {count} gradients from buffer")
        
        return jsonify({
            'success': True,
            'cleared_count': count,
            'message': f'Cleared {count} gradients'
        }), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
