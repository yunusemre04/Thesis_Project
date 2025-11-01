"""
Configuration settings for the Flask API
"""
import os

class Config:
    """Base configuration"""
    # Flask settings
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key-change-in-production'
    DEBUG = False
    TESTING = False
    
    # Model paths
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    MODELS_DIR = os.path.join(BASE_DIR, 'models')
    DL_MODEL_PATH = os.path.join(MODELS_DIR, 'deep_learning_model.h5')
    FL_MODEL_PATH = os.path.join(MODELS_DIR, 'federated_learning_model.h5')
    
    # Dataset path for evaluation
    DATASET_DIR = os.path.join(BASE_DIR, 'UCI_HAR_Dataset')
    
    # Model metadata
    NUM_FEATURES = 561
    NUM_CLASSES = 6
    ACTIVITY_LABELS = [
        'WALKING',
        'WALKING_UPSTAIRS', 
        'WALKING_DOWNSTAIRS',
        'SITTING',
        'STANDING',
        'LAYING'
    ]
    
    # API settings
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16 MB max request size
    JSON_SORT_KEYS = False
    
    # Performance settings
    MODEL_LOAD_TIMEOUT = 30  # seconds
    PREDICTION_TIMEOUT = 10  # seconds

class DevelopmentConfig(Config):
    """Development configuration"""
    DEBUG = True

class ProductionConfig(Config):
    """Production configuration"""
    DEBUG = False

class TestingConfig(Config):
    """Testing configuration"""
    TESTING = True

# Configuration dictionary
config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}

