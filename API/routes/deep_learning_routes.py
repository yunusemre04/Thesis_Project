"""
Deep Learning routes - DL model operations
"""
import os
from flask import Blueprint, request, jsonify, current_app
from utils.validators import validate_request_data

dl_bp = Blueprint('deep_learning', __name__, url_prefix='/api/dl')


@dl_bp.route('/predict', methods=['POST'])
def predict():
    """
    Perform prediction using Deep Learning model
    
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
                "confidence": 95.5,
                "all_probabilities": {...}
            },
            "api_metrics": {
                "cpu_usage_percent": 12.5,
                "ram_usage_mb": 45.2,
                "duration_seconds": 0.123
            },
            "model_type": "Deep Learning"
        }
    """
    try:
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
        
        # Perform prediction
        dl_service = current_app.dl_service
        result, api_metrics = dl_service.predict(sensor_data)
        
        # Add API metrics to response
        result['api_metrics'] = api_metrics
        
        # Return response
        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'model_type': 'Deep Learning'
        }), 500


@dl_bp.route('/info', methods=['GET'])
def get_model_info():
    """
    Get Deep Learning model information
    
    Response JSON:
        {
            "success": true,
            "model_info": {...}
        }
    """
    try:
        model_manager = current_app.model_manager
        model_info = model_manager.get_model_info('dl')
        
        return jsonify({
            'success': True,
            'model_info': model_info
        }), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@dl_bp.route('/evaluate', methods=['GET'])
def evaluate_model():
    """
    Evaluate Deep Learning model on UCI HAR test data
    
    Response JSON:
        {
            "success": true,
            "evaluation": {
                "overall_accuracy": 0.9413,
                "test_samples": 2947,
                "precision_weighted": 0.94,
                "recall_weighted": 0.94,
                "f1_score_weighted": 0.94,
                "per_class_metrics": [...],
                "confusion_matrix": {...}
            }
        }
    """
    try:
        from utils.evaluation import load_uci_har_test_data, evaluate_model, print_evaluation_summary
        
        # Get model
        model_manager = current_app.model_manager
        model = model_manager.get_dl_model()
        
        if model is None:
            return jsonify({
                'success': False,
                'error': 'Deep Learning model not loaded'
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

