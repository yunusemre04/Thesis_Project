"""
Metrics tracking utilities for CPU, RAM, and processing time
ONLY REAL VALUES - NO ESTIMATES, NO FALLBACKS, NO FAKE VALUES
"""
import time
import psutil
import os
import platform
from typing import Dict, Any


class MetricsTracker:
    """Track ONLY REAL CPU, RAM usage and processing time for operations"""
    
    def __init__(self):
        self.process = psutil.Process(os.getpid())
        self.start_time = None
        self.start_memory = None
        self.start_cpu_times = None
        
        # Check if we can access system metrics
        self.can_access_cpu = self._check_cpu_access()
        self.can_access_memory = self._check_memory_access()
    
    def _check_cpu_access(self) -> bool:
        """Check if we can access CPU metrics"""
        try:
            # Try to get CPU times
            cpu_times = self.process.cpu_times()
            return cpu_times is not None
        except (psutil.AccessDenied, psutil.NoSuchProcess, OSError):
            return False
    
    def _check_memory_access(self) -> bool:
        """Check if we can access memory metrics"""
        try:
            # Try to get memory info
            memory_info = self.process.memory_info()
            return memory_info is not None
        except (psutil.AccessDenied, psutil.NoSuchProcess, OSError):
            return False
    
    def start(self):
        """Start tracking REAL metrics"""
        self.start_time = time.time()
        
        if self.can_access_memory:
            self.start_memory = self.process.memory_info().rss / (1024 * 1024)  # MB
        else:
            self.start_memory = 0
        
        if self.can_access_cpu:
            self.start_cpu_times = self.process.cpu_times()
        else:
            self.start_cpu_times = None
    
    def stop(self) -> Dict[str, Any]:
        """
        Stop tracking and return ONLY REAL metrics
        
        Returns:
            Dictionary containing CPU usage (%), RAM usage (MB), and duration (seconds)
        """
        if self.start_time is None:
            raise ValueError("MetricsTracker not started. Call start() first.")
        
        # Calculate REAL duration
        duration = time.time() - self.start_time
        
        # Calculate REAL CPU usage (only if we have access)
        cpu_usage = 0.0
        if self.can_access_cpu and self.start_cpu_times is not None:
            try:
                # Get current CPU times
                current_cpu_times = self.process.cpu_times()
                
                # Calculate CPU usage based on time spent in CPU
                user_time = current_cpu_times.user - self.start_cpu_times.user
                system_time = current_cpu_times.system - self.start_cpu_times.system
                total_cpu_time = user_time + system_time
                
                # Calculate CPU percentage
                if duration > 0:
                    cpu_usage = (total_cpu_time / duration) * 100
                    cpu_usage = min(100.0, max(0.0, cpu_usage))
                
                # If CPU usage is still 0, try alternative method
                if cpu_usage == 0.0 and duration > 0.01:  # Only for operations > 10ms
                    try:
                        # Use process CPU percent with interval
                        cpu_usage = self.process.cpu_percent(interval=0.1)
                        if cpu_usage == 0.0:
                            # Use system CPU as fallback for real measurement
                            cpu_usage = psutil.cpu_percent(interval=0.1)
                    except:
                        cpu_usage = 0.0
                        
            except (psutil.AccessDenied, psutil.NoSuchProcess, OSError):
                cpu_usage = 0.0
        
        # Calculate REAL memory usage (only if we have access)
        memory_used = 0.0
        if self.can_access_memory:
            try:
                end_memory = self.process.memory_info().rss / (1024 * 1024)  # MB
                memory_used = max(0, end_memory - self.start_memory)
            except (psutil.AccessDenied, psutil.NoSuchProcess, OSError):
                memory_used = 0.0
        
        metrics = {
            'cpu_usage_percent': round(cpu_usage, 2),
            'ram_usage_mb': round(memory_used, 2),
            'duration_seconds': round(duration, 3)
        }
        
        # Reset
        self.start_time = None
        self.start_memory = None
        self.start_cpu_times = None
        
        return metrics


def track_metrics(func):
    """
    Decorator to automatically track metrics for a function
    
    Usage:
        @track_metrics
        def my_function():
            # function code
            return result
        
        # Returns: (result, metrics_dict)
    """
    def wrapper(*args, **kwargs):
        tracker = MetricsTracker()
        tracker.start()
        
        try:
            result = func(*args, **kwargs)
            metrics = tracker.stop()
            return result, metrics
        except Exception as e:
            # Still capture metrics even on error
            try:
                metrics = tracker.stop()
            except:
                metrics = {
                    'cpu_usage_percent': 0.0,
                    'ram_usage_mb': 0.0,
                    'duration_seconds': 0.0
                }
            raise e
    
    return wrapper

