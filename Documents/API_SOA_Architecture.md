# SOA Layer Architecture - HAR Model API

## Service-Oriented Architecture (SOA) Layers

This document outlines the SOA layer structure of the Human Activity Recognition Model API based on service-oriented architectural principles.

---

## 1. Business Domain

**Human Activity Recognition & Machine Learning Model Management API**

The API provides machine learning-based human activity recognition services through two different approaches:
- Deep Learning (centralized training)
- Federated Learning (distributed learning)

---

## 2. Business Processes

### a) Deep Learning Prediction Service (Current Service)
- Centralized model inference
- Sensor data preprocessing and validation
- Activity classification using pre-trained DL model
- Performance metrics collection

### b) Federated Learning Operations (Current Service)
- Distributed model prediction
- Client-side model weight updates
- Federated averaging and gradient aggregation
- Privacy-preserving learning

### c) Model Evaluation & Testing (Current Service)
- Model performance assessment
- UCI HAR dataset evaluation
- Comparative analysis (DL vs FL)
- Metrics computation and reporting

### d) Health Monitoring & Configuration (Current Service)
- API health checks
- Model information retrieval
- System status monitoring
- Configuration management

---

## 3. Business Services

### **Prediction Services:**
- **DL Prediction:** Deep Learning model inference (/api/dl/predict)
- **FL Prediction:** Federated Learning model inference (/api/fl/predict)
- **Test DL:** Deep Learning model testing (/api/test/dl)
- **Test FL:** Federated Learning model testing (/api/test/fl)

### **Model Management Services:**
- **Model Loading:** Initialize and cache ML models (ModelManager)
- **Model Information:** Retrieve model metadata and architecture (/api/dl/info, /api/fl/info)
- **Model Update:** FL model weight updates (/api/fl/update_weights, /api/fl/update_weights_gradients)
- **Model Export:** PyTorch and TensorFlow.js model distribution (/api/fl/get_pytorch_model, /api/fl/get_model_tfjs)

### **Evaluation Services:**
- **DL Evaluation:** Deep Learning model assessment on UCI HAR test set (/api/dl/evaluate)
- **FL Evaluation:** Federated Learning model assessment on UCI HAR test set (/api/fl/evaluate)
- **Performance Metrics:** Accuracy, precision, recall, F1-score computation
- **Comparative Analysis:** DL vs FL performance comparison

### **Data Processing Services:**
- **Preprocessing:** Feature scaling, normalization, standardization (StandardScaler)
- **Validation:** Input data validation, shape checking, range validation
- **Metrics Tracking:** API performance metrics (latency, throughput, memory usage)
- **Error Handling:** Request validation, error response formatting

### **Health & Monitoring Services:**
- **Health Check:** API status and availability (/api/health)
- **Models Info:** Overall system information (/api/models/info)
- **Version Management:** API versioning and compatibility

---

## 4. Infrastructure Services

### **Protocols & Communication:**
- **REST API:** RESTful architectural style
- **HTTP/HTTPS:** Communication protocol
- **JSON:** Data interchange format
- **Blueprint Pattern:** Modular route organization (Flask Blueprints)

### **ML Frameworks & Libraries:**
- **TensorFlow 2.x / Keras:** Deep Learning model training and inference
- **PyTorch:** Federated Learning model and gradient operations
- **NumPy:** Numerical computing and array operations
- **Scikit-learn:** Data preprocessing and StandardScaler

### **Data Format & Security:**
- **Request/Response:** JSON format with structured schemas
- **CORS:** Cross-Origin Resource Sharing (Flask-CORS)
- **Input Validation:** Sensor data validation (561 features)
- **Error Handling:** HTTP status codes and error messages

### **Development & Deployment:**
- **Flask 2.x:** Python web framework
- **Werkzeug:** WSGI utility library
- **Python 3.8+:** Programming language
- **Virtual Environment:** Isolated dependency management

---

## 5. Component-based Service Realizations

This layer is divided into three main categories:

### **1. Functional Components:**

#### Model Management:
- **ModelManager:** Centralized model loading, caching, and lifecycle management
  - DL model loading (.h5 format)
  - FL model loading (.h5 and .pt formats)
  - Scaler loading and management (StandardScaler)
  - Model recompilation and optimization

#### Service Layer:
- **DeepLearningService:** DL-specific operations
  - Prediction pipeline
  - Preprocessing and postprocessing
  - Evaluation on test data
- **FederatedService:** FL-specific operations
  - Prediction pipeline
  - Weight update mechanism
  - Real training simulation
- **GradientAggregator:** Federated averaging
  - Gradient extraction from PyTorch model
  - Federated averaging algorithm
  - Weight delta computation

#### Route Controllers:
- **Common Routes:** Health and general endpoints
- **DL Routes:** Deep Learning endpoints
- **FL Routes:** Federated Learning endpoints

### **2. Service Components:**

#### API Endpoints:

**Common Endpoints:**
- `GET /api/health` - Health check
- `GET /api/models/info` - Models information
- `GET /api/test/dl` - Test DL model
- `GET /api/test/fl` - Test FL model

**Deep Learning Endpoints:**
- `POST /api/dl/predict` - DL prediction
- `GET /api/dl/info` - DL model information
- `GET /api/dl/evaluate` - DL model evaluation

**Federated Learning Endpoints:**
- `POST /api/fl/predict` - FL prediction
- `POST /api/fl/update_weights` - Update FL model with training data
- `POST /api/fl/update_weights_gradients` - Update FL model with gradients
- `GET /api/fl/info` - FL model information
- `GET /api/fl/evaluate` - FL model evaluation
- `GET /api/fl/get_pytorch_model` - Download PyTorch model
- `GET /api/fl/pytorch_model_info` - PyTorch model metadata
- `GET /api/fl/get_model_tfjs` - Download TensorFlow.js model
- `GET /api/fl/tfjs_model_info` - TensorFlow.js model metadata

#### Utility Components:

**Preprocessing (preprocessing.py):**
- `preprocess_data()` - Feature scaling and normalization
- `validate_input_shape()` - Shape validation
- `load_test_data()` - UCI HAR test data loading
- `load_scaler()` - StandardScaler loading

**Validators (validators.py):**
- `validate_sensor_data()` - 561-feature validation
- `validate_request_data()` - JSON request validation
- `validate_model_weights()` - Model weight validation
- `validate_true_label()` - Activity label validation

**Evaluation (evaluation.py):**
- `evaluate_model()` - Model performance evaluation
- `compute_metrics()` - Accuracy, precision, recall, F1-score
- `confusion_matrix_analysis()` - Per-class analysis

**Metrics (metrics.py):**
- `MetricsTracker` - API performance tracking
  - Latency measurement
  - Throughput calculation
  - Memory usage monitoring

### **3. Technical Components:**

#### Framework & Libraries:
- **Flask 2.3.0+** - Web application framework
- **TensorFlow 2.12.0+** - Deep Learning framework
- **PyTorch 2.0.0+** - Federated Learning framework
- **NumPy 1.24.0+** - Numerical computing
- **Scikit-learn 1.3.0+** - Machine learning utilities
- **Flask-CORS 4.0.0+** - CORS support

#### Model Formats:
- **Keras H5 (.h5)** - Deep Learning model format
- **PyTorch (.pt)** - Federated Learning model format
- **Pickle (.pkl)** - Scaler serialization format
- **TensorFlow.js** - Browser-compatible model format

#### Configuration:
- **Config Classes:** Development, Production, Testing configurations
- **Environment Variables:** SECRET_KEY, FLASK_ENV
- **Model Paths:** DL_MODEL_PATH, FL_MODEL_PATH, SCALER_PATH
- **Dataset Configuration:** NUM_FEATURES=561, NUM_CLASSES=6

---

## 6. Operational Systems

### **Model Storage & Management:**
- **Deep Learning Model Storage:** `models/deep_learning_model.h5` (TensorFlow/Keras format)
- **Federated Learning Model Storage:** `models/federated_learning_model.h5` and `models/fl_model.pt`
- **Scaler Storage:** `models/scaler_dl.pkl` and `models/scaler_fl.pkl` (Scikit-learn StandardScaler)
- **Model Versioning:** Version control for model updates and rollbacks

### **Dataset Management:**
- **UCI HAR Dataset:** Human Activity Recognition Using Smartphones Dataset
  - Training Set: 7,352 samples, 561 features
  - Test Set: 2,947 samples, 561 features
  - Activity Labels: WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING
  - Inertial Signals: Raw accelerometer and gyroscope data

### **Metrics Collection & Monitoring:**
- **API Performance Metrics:**
  - Request latency tracking
  - Response time measurement
  - Error rate monitoring
  - Throughput calculation
- **Model Performance Metrics:**
  - Prediction accuracy
  - Confidence scores
  - Per-class probabilities
  - Confusion matrix analysis

### **Logging & Error Tracking:**
- **Application Logging:** Flask logging system
- **Model Loading Logs:** Model initialization and loading status
- **Request/Response Logging:** API endpoint access logs
- **Error Handling:** Exception tracking and error responses

### **Training Management:**
- **Federated Averaging History:** Gradient aggregation tracking
- **Weight Update History:** FL model update logs
- **Training Metrics:** Loss, accuracy per update round
- **Client Contribution Tracking:** Number of updates per client simulation

### **Caching & Optimization:**
- **Model Caching:** Pre-loaded models in memory
- **Scaler Caching:** Pre-loaded StandardScalers
- **Eager Execution:** TensorFlow eager mode for debugging
- **Model Compilation:** Optimizer configuration for inference

---

## 📋 SOA Layer Summary

| Layer | Key Components | Technology Stack |
|-------|---------------|------------------|
| **1. Business Domain** | HAR & ML Model Management | - |
| **2. Business Processes** | DL Prediction, FL Operations, Evaluation, Health Monitoring | Python, Flask |
| **3. Business Services** | Prediction, Model Management, Evaluation, Data Processing, Health | REST API |
| **4. Infrastructure Services** | REST, HTTP/HTTPS, JSON, TensorFlow, PyTorch, NumPy, Scikit-learn | Flask, TF 2.x, PyTorch |
| **5. Service Realizations** | ModelManager, Services, Routes, Utilities | Flask Blueprints, Python |
| **6. Operational Systems** | Model Storage, Dataset, Metrics, Logging, Caching | File System, Memory |

---

## 🔄 Service Interaction Flow

```
Client Request (Mobile App)
    ↓
[HTTP POST /api/dl/predict or /api/fl/predict]
    ↓
Flask API Gateway (Route Blueprints)
    ↓
Request Validation (validators.py)
    ↓
Service Layer (DeepLearningService / FederatedService)
    ↓
Data Preprocessing (preprocessing.py)
    ↓
Model Manager (ModelManager)
    ↓
ML Model Inference (TensorFlow/PyTorch)
    ↓
Post-processing & Metrics (evaluation.py, metrics.py)
    ↓
JSON Response Formation
    ↓
Client Response (Activity Classification)
```

---

## 🎯 SOA Design Principles Applied

1. **Service Abstraction:** Each service has well-defined interfaces and hides implementation details
2. **Service Reusability:** Services are designed for reuse across different endpoints
3. **Service Autonomy:** Each service manages its own lifecycle and resources
4. **Service Statelessness:** API endpoints are stateless (models cached in app context)
5. **Service Discoverability:** API provides metadata and documentation endpoints
6. **Service Composability:** Services can be combined for complex operations
7. **Service Loose Coupling:** Services communicate through standard REST interfaces

---

**Document Version:** 1.0  
**Created:** February 16, 2026  
**Project:** Human Activity Recognition API - Deep Learning vs Federated Learning  
**API Version:** 1.0.0
