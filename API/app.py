"""
Main Flask application
"""
from flask import Flask, send_from_directory
from flask_cors import CORS
from config import config
from services import ModelManager, DeepLearningService, FederatedService
from services.gradient_aggregator import GradientAggregator
from routes import common_bp, dl_bp, fl_bp
import os
import tensorflow as tf

# Ensure TensorFlow eager execution is enabled
tf.config.run_functions_eagerly(True)

def create_app(config_name='default'):
    """
    Application factory
    
    Args:
        config_name: Configuration name ('development', 'production', 'testing')
    
    Returns:
        Flask application instance
    """
    # Create Flask app
    app = Flask(__name__)
    
    # Load configuration
    app.config.from_object(config[config_name])
    
    # Enable CORS for mobile app communication
    CORS(app)
    
    # Initialize services
    with app.app_context():
        # Create model manager
        model_manager = ModelManager(app.config)
        
        # Load models
        try:
            model_manager.load_models()
        except Exception as e:
            print(f"Warning: Failed to load models: {str(e)}")
        
        # Create services
        dl_service = DeepLearningService(model_manager, app.config)
        fl_service = FederatedService(model_manager, app.config)
        
        # Create gradient aggregator for FL
        pytorch_model_path = os.path.join(app.config['MODELS_DIR'], 'fl_model.pt')
        gradient_aggregator = GradientAggregator(
            model_path=pytorch_model_path,
            learning_rate=0.001
        )
        
        # Store services in app context
        app.model_manager = model_manager
        app.dl_service = dl_service
        app.fl_service = fl_service
        app.gradient_aggregator = gradient_aggregator
    
    # Register blueprints
    app.register_blueprint(common_bp)
    app.register_blueprint(dl_bp)
    app.register_blueprint(fl_bp)
    
    
    # Error handlers
    @app.errorhandler(404)
    def not_found(error):
        return {
            'success': False,
            'error': 'Endpoint not found'
        }, 404
    
    @app.errorhandler(500)
    def internal_error(error):
        return {
            'success': False,
            'error': 'Internal server error'
        }, 500
    
    @app.errorhandler(413)
    def request_too_large(error):
        return {
            'success': False,
            'error': 'Request payload too large'
        }, 413
    
    # Root endpoint
    @app.route('/')
    def index():
        return {
            'message': 'HAR Model API',
            'version': '1.0.0',
            'endpoints': {
                'health': '/api/health',
                'test_dl': '/api/test/dl',
                'test_fl': '/api/test/fl',
                'models_info': '/api/models/info',
                'dl_predict': '/api/dl/predict',
                'dl_info': '/api/dl/info',
                'dl_evaluate': '/api/dl/evaluate',
                'fl_predict': '/api/fl/predict',
                'fl_update': '/api/fl/update_weights',
                'fl_info': '/api/fl/info',
                'fl_evaluate': '/api/fl/evaluate',
                'fl_get_model_tfjs': '/api/fl/get_model_tfjs',
                'fl_tfjs_model_info': '/api/fl/tfjs_model_info',
                'fl_get_pytorch_model': '/api/fl/get_pytorch_model',
                'fl_pytorch_model_info': '/api/fl/pytorch_model_info',
                'fl_update_weights_gradients': '/api/fl/update_weights_gradients'
            }
        }
    
    return app


if __name__ == '__main__':
    # Get configuration from environment or use default
    config_name = os.environ.get('FLASK_ENV', 'development')
    
    # Create app
    app = create_app(config_name)
    
    # Run app
    print("\n" + "="*60)
    print("HAR Model API Server Starting...")
    print("="*60)
    print(f"Environment: {config_name}")
    print(f"Debug: {app.config['DEBUG']}")
    print("="*60 + "\n")
    
    app.run(
        host='0.0.0.0',
        port=5000,
        debug=app.config['DEBUG']
    )

