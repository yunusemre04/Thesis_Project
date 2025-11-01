"""
Common routes - Health check and model information
"""
import os
from flask import Blueprint, jsonify, current_app

common_bp = Blueprint('common', __name__, url_prefix='/api')


@common_bp.route('/health', methods=['GET'])
def health_check():
    """
    API health check endpoint
    
    Response JSON:
        {
            "status": "healthy",
            "message": "API is running",
            "timestamp": "2024-01-01T12:00:00",
            "version": "1.0.0"
        }
    """
    from datetime import datetime
    
    return jsonify({
        'status': 'healthy',
        'message': 'API is running',
        'timestamp': datetime.now().isoformat(),
        'version': '1.0.0'
    }), 200


@common_bp.route('/models/info', methods=['GET'])
def get_all_models_info():
    """
    Get information about all models with optional evaluation
    
    Query Parameters:
        evaluate: Set to 'true' to include test accuracy (default: false)
    
    Response JSON:
        {
            "success": true,
            "models": {
                "deep_learning": {
                    "model_info": {...},
                    "evaluation": {...}  // if evaluate=true
                },
                "federated_learning": {
                    "model_info": {...},
                    "evaluation": {...}  // if evaluate=true
                }
            }
        }
    """
    try:
        from flask import request
        from utils.evaluation import load_uci_har_test_data, evaluate_model
        
        # Check if evaluation is requested
        should_evaluate = request.args.get('evaluate', 'false').lower() == 'true'
        
        model_manager = current_app.model_manager
        
        # Get basic model info
        dl_info = model_manager.get_model_info('dl')
        fl_info = model_manager.get_model_info('fl')
        
        result = {
            'success': True,
            'models': {
                'deep_learning': {
                    'model_info': dl_info
                },
                'federated_learning': {
                    'model_info': fl_info
                }
            }
        }
        
        # Add evaluation if requested and dataset available
        if should_evaluate:
            dataset_path = current_app.config.get('DATASET_DIR')
            
            if dataset_path and os.path.exists(dataset_path):
                try:
                    print("Loading test data for evaluation...")
                    X_test, y_test = load_uci_har_test_data(dataset_path)
                    activity_labels = current_app.config.get('ACTIVITY_LABELS')
                    
                    # Evaluate DL model
                    dl_model = model_manager.get_dl_model()
                    if dl_model:
                        print("Evaluating Deep Learning model...")
                        dl_eval = evaluate_model(dl_model, X_test, y_test, activity_labels)
                        result['models']['deep_learning']['evaluation'] = dl_eval
                    
                    # Evaluate FL model
                    fl_model = model_manager.get_fl_model()
                    if fl_model:
                        print("Evaluating Federated Learning model...")
                        fl_eval = evaluate_model(fl_model, X_test, y_test, activity_labels)
                        result['models']['federated_learning']['evaluation'] = fl_eval
                    
                    result['evaluation_note'] = 'Test accuracy computed on UCI HAR test set (2947 samples)'
                    
                except Exception as e:
                    result['evaluation_error'] = str(e)
                    result['evaluation_note'] = 'Evaluation failed - using model info only'
            else:
                result['evaluation_note'] = 'Dataset not available - using model info only'
        
        return jsonify(result), 200
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@common_bp.route('/test/dl', methods=['GET'])
def test_dl_model():
    """
    Test Deep Learning model health
    
    Response JSON:
        {
            "success": true,
            "message": "Deep Learning model is healthy",
            "model_info": {...}
        }
    """
    try:
        dl_service = current_app.dl_service
        result = dl_service.test_model()
        
        status_code = 200 if result['success'] else 500
        return jsonify(result), status_code
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@common_bp.route('/test/fl', methods=['GET'])
def test_fl_model():
    """
    Test Federated Learning model health
    
    Response JSON:
        {
            "success": true,
            "message": "Federated Learning model is healthy",
            "model_info": {...}
        }
    """
    try:
        fl_service = current_app.fl_service
        result = fl_service.test_model()
        
        status_code = 200 if result['success'] else 500
        return jsonify(result), status_code
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
