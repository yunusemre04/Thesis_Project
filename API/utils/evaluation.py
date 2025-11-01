"""
Model evaluation utilities
Load test data and evaluate model performance
"""
import numpy as np
import os
from typing import Dict, Any, Tuple
from sklearn.metrics import accuracy_score, precision_recall_fscore_support, confusion_matrix


def load_uci_har_test_data(dataset_path: str) -> Tuple[np.ndarray, np.ndarray]:
    """
    Load UCI HAR Dataset test data
    
    Args:
        dataset_path: Path to UCI_HAR_Dataset folder
    
    Returns:
        Tuple of (X_test, y_test)
    """
    try:
        # Load test features
        X_test_path = os.path.join(dataset_path, 'test', 'X_test.txt')
        X_test = np.loadtxt(X_test_path)
        
        # Load test labels
        y_test_path = os.path.join(dataset_path, 'test', 'y_test.txt')
        y_test = np.loadtxt(y_test_path)
        
        # Convert labels to 0-indexed (UCI HAR uses 1-6, we need 0-5)
        y_test = y_test - 1
        
        print(f"✓ Test data loaded: {X_test.shape[0]} samples, {X_test.shape[1]} features")
        
        return X_test, y_test
        
    except FileNotFoundError as e:
        raise FileNotFoundError(
            f"UCI HAR Dataset not found at {dataset_path}. "
            f"Please ensure test/X_test.txt and test/y_test.txt exist."
        ) from e
    except Exception as e:
        raise Exception(f"Error loading test data: {str(e)}") from e


def evaluate_model(model, X_test: np.ndarray, y_test: np.ndarray, 
                   activity_labels: list, preprocess: bool = True) -> Dict[str, Any]:
    """
    Evaluate model on test data
    
    Args:
        model: Keras model to evaluate
        X_test: Test features (raw, will be preprocessed)
        y_test: Test labels (0-indexed)
        activity_labels: List of activity names
        preprocess: Whether to preprocess the data (default: True)
    
    Returns:
        Dictionary with evaluation metrics
    """
    try:
        # Preprocess test data (CRITICAL - must match training preprocessing!)
        if preprocess:
            print("Preprocessing test data (standardization)...")
            # Apply same preprocessing as training
            # StandardScaler: (X - mean) / std
            X_test_mean = np.mean(X_test, axis=0, keepdims=True)
            X_test_std = np.std(X_test, axis=0, keepdims=True) + 1e-8  # Avoid division by zero
            X_test_processed = (X_test - X_test_mean) / X_test_std
            print(f"✓ Data preprocessed (standardized)")
        else:
            X_test_processed = X_test
        
        # Make predictions
        print("Making predictions on test set...")
        predictions = model.predict(X_test_processed, verbose=0)
        y_pred = np.argmax(predictions, axis=1)
        
        # Calculate overall accuracy
        accuracy = accuracy_score(y_test, y_pred)
        
        # Calculate per-class metrics
        precision, recall, f1, support = precision_recall_fscore_support(
            y_test, y_pred, average=None, zero_division=0
        )
        
        # Calculate weighted averages
        precision_avg, recall_avg, f1_avg, _ = precision_recall_fscore_support(
            y_test, y_pred, average='weighted', zero_division=0
        )
        
        # Confusion matrix
        cm = confusion_matrix(y_test, y_pred)
        
        # Per-class accuracy
        per_class_accuracy = cm.diagonal() / cm.sum(axis=1)
        
        # Build per-class results
        per_class_results = []
        for idx, label in enumerate(activity_labels):
            per_class_results.append({
                'activity': label,
                'precision': float(precision[idx]),
                'recall': float(recall[idx]),
                'f1_score': float(f1[idx]),
                'accuracy': float(per_class_accuracy[idx]),
                'support': int(support[idx])
            })
        
        # Build confusion matrix with labels
        confusion_matrix_labeled = {
            'labels': activity_labels,
            'matrix': cm.tolist()
        }
        
        results = {
            'overall_accuracy': float(accuracy),
            'test_samples': int(len(y_test)),
            'precision_weighted': float(precision_avg),
            'recall_weighted': float(recall_avg),
            'f1_score_weighted': float(f1_avg),
            'per_class_metrics': per_class_results,
            'confusion_matrix': confusion_matrix_labeled
        }
        
        print(f"✓ Evaluation complete: Accuracy = {accuracy*100:.2f}%")
        
        return results
        
    except Exception as e:
        raise Exception(f"Error during evaluation: {str(e)}") from e


def print_evaluation_summary(results: Dict[str, Any]):
    """
    Print a formatted summary of evaluation results
    
    Args:
        results: Evaluation results dictionary
    """
    print("\n" + "="*60)
    print("MODEL EVALUATION SUMMARY")
    print("="*60)
    print(f"Test Samples: {results['test_samples']}")
    print(f"Overall Accuracy: {results['overall_accuracy']*100:.2f}%")
    print(f"Precision (weighted): {results['precision_weighted']*100:.2f}%")
    print(f"Recall (weighted): {results['recall_weighted']*100:.2f}%")
    print(f"F1-Score (weighted): {results['f1_score_weighted']*100:.2f}%")
    print("\nPer-Class Performance:")
    print("-"*60)
    print(f"{'Activity':<20} {'Accuracy':<10} {'Precision':<10} {'Recall':<10} {'F1':<10}")
    print("-"*60)
    
    for cls in results['per_class_metrics']:
        print(f"{cls['activity']:<20} "
              f"{cls['accuracy']*100:>8.2f}% "
              f"{cls['precision']*100:>8.2f}% "
              f"{cls['recall']*100:>8.2f}% "
              f"{cls['f1_score']*100:>8.2f}%")
    
    print("="*60 + "\n")

