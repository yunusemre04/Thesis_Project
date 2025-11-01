"""
API route modules
"""
from .common_routes import common_bp
from .deep_learning_routes import dl_bp
from .federated_routes import fl_bp

__all__ = ['common_bp', 'dl_bp', 'fl_bp']

