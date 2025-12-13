# Human Activity Recognition System

A mobile human activity recognition system implementing both **Federated Learning** and **Deep Learning** approaches for privacy-preserving on-device inference and model training.

[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)
[![TensorFlow](https://img.shields.io/badge/TensorFlow-2.13+-orange.svg)](https://www.tensorflow.org/)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.0+-red.svg)](https://pytorch.org/)
[![Flask](https://img.shields.io/badge/Flask-3.0+-green.svg)](https://flask.palletsprojects.com/)
[![Android](https://img.shields.io/badge/Android-8.0+-brightgreen.svg)](https://developer.android.com/)

## 📖 Overview

This project implements a complete federated learning system for human activity recognition using smartphone sensor data. It demonstrates privacy-preserving machine learning where models are trained on-device without sending raw sensor data to a central server.

### Key Features

- 🔒 **Privacy-First Design**: On-device inference with PyTorch Mobile
- 📱 **Android Mobile App**: Real-time activity recognition with sensor data collection
- 🤖 **Dual Approach**: Both Federated Learning (FL) and Deep Learning (DL) implementations
- 🔄 **Federated Training**: Local model updates with gradient aggregation
- 📊 **Performance Metrics**: CPU, RAM, and accuracy tracking
- 🎯 **6 Activities**: Walking, Walking Upstairs, Walking Downstairs, Sitting, Standing, Laying
- 🌐 **REST API**: Flask-based server for model serving and aggregation

## 🏗️ Project Structure

```
Thesis_Project/
├── API/                          # Flask REST API server
│   ├── app.py                   # Main Flask application
│   ├── config.py                # Configuration settings
│   ├── routes/                  # API endpoints
│   │   ├── common_routes.py     # Shared endpoints
│   │   ├── deep_learning_routes.py
│   │   └── federated_routes.py
│   ├── services/                # Business logic
│   │   ├── deep_learning_service.py
│   │   ├── federated_service.py
│   │   ├── gradient_aggregator.py
│   │   └── model_manager.py
│   ├── utils/                   # Helper functions
│   ├── models/                  # Trained models
│   │   ├── deep_learning_model.h5
│   │   ├── federated_learning_model.h5
│   │   ├── fl_model.pt         # PyTorch mobile model
│   │   ├── scaler_dl.pkl
│   │   └── scaler_fl.pkl
│   └── UCI_HAR_Dataset/        # Test dataset
│
├── Mobile_App/                  # Android application
│   └── app/src/main/
│       ├── java/com/example/positiondeterminer/
│       │   ├── data/           # Data models & API service
│       │   ├── ui/             # UI components & screens
│       │   ├── viewmodel/      # ViewModels (MVVM)
│       │   └── MainActivity.kt
│       └── assets/
│           └── fl_model.pt     # PyTorch model for on-device inference
│
├── Model Training/              # Jupyter notebooks
│   ├── deep_learning_training.ipynb
│   ├── federated_learning_training.ipynb
│   └── UCI_HAR_Dataset/        # Training dataset
│
└── Documents/                   # Project documentation
    ├── Performance_Metrics.md
    ├── Risk_Analysis.md
    └── SWOT_Analysis.md
```

## 🚀 Getting Started

### Prerequisites

**Backend (API):**
- Python 3.11+
- TensorFlow 2.13+
- PyTorch 2.0+
- Flask 3.0+
- NumPy, scikit-learn, pandas

**Mobile App:**
- Android Studio Arctic Fox or later
- Android SDK 26+ (Android 8.0+)
- Kotlin 1.9+
- Gradle 8.0+

**Model Training:**
- Jupyter Notebook
- Google Colab (recommended)
- UCI HAR Dataset

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/yunusemre04/Thesis_Project.git
cd Thesis_Project
```

#### 2. Setup API Server

```bash
cd API

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install flask tensorflow torch scikit-learn pandas numpy joblib psutil

# Run the server
python app.py
```

The API will be available at `http://localhost:5000`

#### 3. Setup Mobile App

```bash
cd Mobile_App

# Open in Android Studio
# File > Open > Select Mobile_App directory

# Sync Gradle
# Build > Rebuild Project

# Run on device/emulator
# Run > Run 'app'
```

**Important:** Update the API URL in `ApiService.kt` to match your server IP:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:5000/api/"
```

## 📊 Architecture

### Federated Learning Workflow

```
┌─────────────┐
│   Mobile    │  1. Collect sensor data
│   Device    │  2. Local prediction (PyTorch)
│             │  3. Compute gradients
└──────┬──────┘
       │ 4. Send gradients (not raw data!)
       ▼
┌─────────────┐
│  API Server │  5. Aggregate gradients (FedAvg)
│             │  6. Update global model
│             │  7. Send updated model
└──────┬──────┘
       │ 8. Download new model
       ▼
┌─────────────┐
│   Mobile    │  9. Update local model
│   Device    │  10. Repeat
└─────────────┘
```

### Deep Learning Workflow

```
┌─────────────┐
│   Mobile    │  1. Collect sensor data
│   Device    │  2. Send to server
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  API Server │  3. Preprocess with scaler
│             │  4. Model prediction
│             │  5. Return result
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Mobile    │  6. Display result
│   Device    │
└─────────────┘
```

## 🎯 API Endpoints

### Common Endpoints
- `GET /api/health` - Health check
- `GET /api/models/info` - Model information

### Deep Learning
- `POST /api/dl/predict` - Predict activity
- `GET /api/dl/evaluate` - Evaluate model accuracy

### Federated Learning
- `POST /api/fl/predict` - Predict activity (H5 model)
- `POST /api/fl/update-weights` - Update global model with gradients
- `GET /api/fl/model/download` - Download PyTorch model
- `GET /api/fl/model/info` - Model metadata
- `POST /api/fl/pytorch/predict` - Predict with PyTorch model
- `GET /api/fl/evaluate` - Evaluate model accuracy

## 📱 Mobile App Features

### Screens

1. **Home Screen**
   - Model selection (FL/DL)
   - Quick stats overview

2. **Deep Learning Screen**
   - Server-based prediction
   - Real-time metrics (CPU, RAM, duration)
   - Confidence scores

3. **Federated Learning Screen**
   - On-device prediction with PyTorch Mobile
   - Model download/update
   - Local training with gradient computation
   - Full FL flow with timing metrics

### Supported Activities

1. 🚶 Walking
2. 🏃 Walking Upstairs
3. 🏃 Walking Downstairs
4. 🪑 Sitting
5. 🧍 Standing
6. 🛏️ Laying

## 📈 Performance Metrics

### Model Comparison

| Metric | FL (Local) | FL (w/ Update) | Deep Learning |
|--------|------------|----------------|---------------|
| **CPU Usage** | 44.26% | 37.40% | 43.79% |
| **RAM Usage** | 159.46 MB | 148.50 MB | 169.04 MB |
| **Duration** | 2.79s | 4.48s | 3.34s |
| **Accuracy** | 93.72% | 93.72% | 96.13% |
| **Training Time** | 9m 48s | 9m 48s | 32s |

### Key Insights

- ✅ **Privacy**: FL keeps data on-device
- ⚡ **Speed**: FL local prediction is fastest (2.79s)
- 🎯 **Accuracy**: DL slightly higher (96.13% vs 93.72%)
- 💾 **Efficiency**: FL with updates uses least RAM (148.50 MB)

## 🔬 Model Training

### Dataset: UCI HAR

- **Source**: [UCI Machine Learning Repository](https://archive.ics.uci.edu/ml/datasets/human+activity+recognition+using+smartphones)
- **Samples**: 10,299 (7,352 training, 2,947 test)
- **Features**: 561 time and frequency domain features
- **Subjects**: 30 volunteers (19-48 years)
- **Classes**: 6 activities

### Training Process

1. **Data Preprocessing**
   - StandardScaler normalization
   - Train/test split (70/30)

2. **Model Architecture** (Same for FL & DL)
   ```
   Input (561) → Dense(512) → BatchNorm → Dropout(0.5)
              → Dense(256) → BatchNorm → Dropout(0.4)
              → Dense(128) → BatchNorm → Dropout(0.3)
              → Dense(64)  → BatchNorm → Dropout(0.2)
              → Output(6)
   ```

3. **Training Configuration**
   - **DL**: 100 epochs, early stopping (patience=20), batch=32
   - **FL**: 100 rounds, 10 clients/round, 5 local epochs

4. **Conversion to PyTorch**
   - H5 → PyTorch state_dict
   - TorchScript for mobile deployment

## 🛠️ Technologies

### Backend
- **Flask**: REST API framework
- **TensorFlow/Keras**: Deep learning models
- **PyTorch**: Federated learning & mobile deployment
- **scikit-learn**: Data preprocessing
- **NumPy**: Numerical computing

### Mobile
- **Kotlin**: Primary language
- **Jetpack Compose**: Modern UI toolkit
- **PyTorch Mobile**: On-device inference
- **Retrofit**: API client
- **Coroutines**: Asynchronous operations
- **ViewModel**: MVVM architecture

### Training
- **Jupyter Notebook**: Interactive development
- **Google Colab**: Cloud GPU training
- **Matplotlib/Seaborn**: Visualization

## 🔐 Privacy & Security

- ✅ **No Raw Data Transfer**: Only gradients sent to server
- ✅ **On-Device Processing**: PyTorch Mobile inference
- ✅ **Encrypted Communication**: HTTPS (production)
- ✅ **Local Storage**: Models stored securely in app assets
- ✅ **User Consent**: Explicit permission for data collection

## 📝 Development

### Running Tests

```bash
# API tests
cd API
python -m pytest tests/

# Android tests
cd Mobile_App
./gradlew test
```

### Building for Production

**API:**
```bash
# Use production server (e.g., Gunicorn)
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

**Mobile:**
```bash
# Build release APK
cd Mobile_App
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

## 📄 License

This project is part of a Master's thesis and is available for academic and educational purposes.

## 👥 Authors

- **Yunus Emre** - [@yunusemre04](https://github.com/yunusemre04)

## 🙏 Acknowledgments

- UCI Machine Learning Repository for the HAR dataset
- TensorFlow and PyTorch teams
- Android Jetpack Compose team
- Academic advisors and reviewers

## 📧 Contact

For questions or collaboration:
- GitHub: [@yunusemre04](https://github.com/yunusemre04)
- Project Link: [https://github.com/yunusemre04/Thesis_Project](https://github.com/yunusemre04/Thesis_Project)

---

**Note**: This is a research project. For production use, additional security measures, testing, and optimization are recommended.
