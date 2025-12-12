"""
Performance Metrics Visualization
Comparison of FL (Local), FL with Weight Updates, and DL approaches
"""

import matplotlib.pyplot as plt
import numpy as np

# Set style for better-looking plots
plt.style.use('seaborn-v0_8-darkgrid')

# Define the methods
methods = ['FL (Local)', 'FL with\nWeight Updates', 'DL']
x_pos = np.arange(len(methods))

# Performance data
cpu_usage = [44.256, 37.396, 43.789]  # CPU (%)
ram_usage = [159.46, 148.5, 169.04]   # RAM (MB)
duration = [2.78, 4.47, 3.34]     # Duration (seconds)

# Create figure with 3 subplots
fig, axes = plt.subplots(1, 3, figsize=(18, 5))

# Color scheme
colors = ['#2E86AB', '#A23B72', '#F18F01']

# ========== CPU Usage Plot ==========
bars1 = axes[0].bar(x_pos, cpu_usage, color='#2E86AB', alpha=0.8, edgecolor='black', linewidth=1.5)
axes[0].set_title('CPU Usage Comparison', fontsize=14, fontweight='bold', pad=15)
axes[0].set_xlabel('Method', fontsize=12, fontweight='bold')
axes[0].set_ylabel('CPU Usage (%)', fontsize=12, fontweight='bold')
axes[0].set_xticks(x_pos)
axes[0].set_xticklabels(methods, fontsize=10)
axes[0].grid(True, alpha=0.3, linestyle='--', axis='y')
axes[0].set_ylim([0, 50])

# Add value labels on bars
for bar in bars1:
    height = bar.get_height()
    axes[0].text(bar.get_x() + bar.get_width()/2., height,
                 f'{height:.2f}%',
                 ha='center', va='bottom',
                 fontweight='bold', fontsize=10)

# ========== RAM Usage Plot ==========
bars2 = axes[1].bar(x_pos, ram_usage, color='#A23B72', alpha=0.8, edgecolor='black', linewidth=1.5)
axes[1].set_title('RAM Usage Comparison', fontsize=14, fontweight='bold', pad=15)
axes[1].set_xlabel('Method', fontsize=12, fontweight='bold')
axes[1].set_ylabel('RAM Usage (MB)', fontsize=12, fontweight='bold')
axes[1].set_xticks(x_pos)
axes[1].set_xticklabels(methods, fontsize=10)
axes[1].grid(True, alpha=0.3, linestyle='--', axis='y')
axes[1].set_ylim([0, 180])

# Add value labels on bars
for bar in bars2:
    height = bar.get_height()
    axes[1].text(bar.get_x() + bar.get_width()/2., height,
                 f'{height:.2f} MB',
                 ha='center', va='bottom',
                 fontweight='bold', fontsize=10)

# ========== Duration Plot ==========
bars3 = axes[2].bar(x_pos, duration, color='#F18F01', alpha=0.8, edgecolor='black', linewidth=1.5)
axes[2].set_title('Prediction Duration Comparison', fontsize=14, fontweight='bold', pad=15)
axes[2].set_xlabel('Method', fontsize=12, fontweight='bold')
axes[2].set_ylabel('Duration (seconds)', fontsize=12, fontweight='bold')
axes[2].set_xticks(x_pos)
axes[2].set_xticklabels(methods, fontsize=10)
axes[2].grid(True, alpha=0.3, linestyle='--', axis='y')
axes[2].set_ylim([0, 5])

# Add value labels on bars
for bar in bars3:
    height = bar.get_height()
    axes[2].text(bar.get_x() + bar.get_width()/2., height,
                 f'{height:.3f}s',
                 ha='center', va='bottom',
                 fontweight='bold', fontsize=10)

plt.tight_layout()
plt.savefig('performance_metrics_comparison.png', dpi=300, bbox_inches='tight')
print("✓ Saved: performance_metrics_comparison.png")
plt.show()

# ========== Accuracy Comparison ==========
fig, ax = plt.subplots(figsize=(10, 6))

models = ['Federated Learning', 'Deep Learning']
accuracy = [93.72, 94.64]
training_duration_seconds = [9*60 + 48, 32]  # Convert to seconds

x_pos_acc = np.arange(len(models))

# Plot accuracy
bars_acc = ax.bar(x_pos_acc, accuracy, color='#06A77D', alpha=0.8, edgecolor='black', linewidth=1.5)

ax.set_title('Model Accuracy Comparison', fontsize=16, fontweight='bold', pad=20)
ax.set_xlabel('Model Type', fontsize=13, fontweight='bold')
ax.set_ylabel('Accuracy (%)', fontsize=13, fontweight='bold')
ax.set_xticks(x_pos_acc)
ax.set_xticklabels(models, fontsize=11)
ax.grid(True, alpha=0.3, linestyle='--', axis='y')
ax.set_ylim([0, 100])

# Add value labels on bars
for bar in bars_acc:
    height = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2., height,
            f'{height:.2f}%',
            ha='center', va='bottom',
            fontweight='bold', fontsize=11)

plt.tight_layout()
plt.savefig('accuracy_comparison.png', dpi=300, bbox_inches='tight')
print("✓ Saved: accuracy_comparison.png")
plt.show()

# ========== Training Duration Comparison ==========
fig, ax = plt.subplots(figsize=(10, 6))

# Plot training duration
bars_duration = ax.bar(x_pos_acc, training_duration_seconds, color='#D62828', alpha=0.8, edgecolor='black', linewidth=1.5)

ax.set_title('Model Training Duration Comparison', fontsize=16, fontweight='bold', pad=20)
ax.set_xlabel('Model Type', fontsize=13, fontweight='bold')
ax.set_ylabel('Training Duration (seconds)', fontsize=13, fontweight='bold')
ax.set_xticks(x_pos_acc)
ax.set_xticklabels(models, fontsize=11)
ax.grid(True, alpha=0.3, linestyle='--', axis='y')
ax.set_ylim([0, max(training_duration_seconds) * 1.2])

# Add value labels on bars
training_labels = ['9 min 48 sec', '32 sec']
for bar, label, value in zip(bars_duration, training_labels, training_duration_seconds):
    height = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2., height,
            f'{label}\n({value}s)',
            ha='center', va='bottom',
            fontweight='bold', fontsize=11)

plt.tight_layout()
plt.savefig('training_duration_comparison.png', dpi=300, bbox_inches='tight')
print("✓ Saved: training_duration_comparison.png")
plt.show()

# ========== Combined Accuracy and Training Duration ==========
fig, ax1 = plt.subplots(figsize=(12, 7))

# Set bar width and positions
bar_width = 0.35
x_pos_combined = np.arange(len(models))

# Plot accuracy bars on left y-axis
bars_acc_combined = ax1.bar(x_pos_combined - bar_width/2, accuracy, bar_width, 
                             color='#06A77D', alpha=0.8, edgecolor='black', linewidth=1.5,
                             label='Accuracy (%)')
ax1.set_xlabel('Model Type', fontsize=13, fontweight='bold')
ax1.set_ylabel('Accuracy (%)', fontsize=13, fontweight='bold', color='#06A77D')
ax1.set_xticks(x_pos_combined)
ax1.set_xticklabels(models, fontsize=11)
ax1.tick_params(axis='y', labelcolor='#06A77D')
ax1.set_ylim([0, 100])
ax1.grid(True, alpha=0.3, linestyle='--', axis='y')

# Add value labels for accuracy bars
for bar in bars_acc_combined:
    height = bar.get_height()
    ax1.text(bar.get_x() + bar.get_width()/2., height,
             f'{height:.2f}%',
             ha='center', va='bottom',
             fontweight='bold', fontsize=10,
             color='#06A77D')

# Create second y-axis for training duration
ax2 = ax1.twinx()
bars_dur_combined = ax2.bar(x_pos_combined + bar_width/2, training_duration_seconds, bar_width,
                             color='#D62828', alpha=0.8, edgecolor='black', linewidth=1.5,
                             label='Training Duration (seconds)')
ax2.set_ylabel('Training Duration (seconds)', fontsize=13, fontweight='bold', color='#D62828')
ax2.tick_params(axis='y', labelcolor='#D62828')
ax2.set_ylim([0, max(training_duration_seconds) * 1.2])

# Add value labels for training duration bars
for bar, label in zip(bars_dur_combined, training_labels):
    height = bar.get_height()
    ax2.text(bar.get_x() + bar.get_width()/2., height,
             f'{label}',
             ha='center', va='bottom',
             fontweight='bold', fontsize=10,
             color='#D62828')

# Add title and legend
ax1.set_title('Model Performance: Accuracy vs Training Duration', 
              fontsize=16, fontweight='bold', pad=20)

# Combine legends
lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper center', 
           bbox_to_anchor=(0.5, -0.1), ncol=2, fontsize=11, frameon=True)

plt.tight_layout()
plt.savefig('combined_accuracy_training_duration.png', dpi=300, bbox_inches='tight')
print("✓ Saved: combined_accuracy_training_duration.png")
plt.show()

# ========== Summary Statistics ==========
print("\n" + "="*60)
print("PERFORMANCE METRICS SUMMARY")
print("="*60)

print("\n1. CPU USAGE (%)")
print("-" * 60)
for method, cpu in zip(methods, cpu_usage):
    print(f"  {method:25s}: {cpu:6.3f}%")
print(f"  Best (Lowest): FL with Weight Updates ({min(cpu_usage):.3f}%)")

print("\n2. RAM USAGE (MB)")
print("-" * 60)
for method, ram in zip(methods, ram_usage):
    print(f"  {method:25s}: {ram:7.2f} MB")
print(f"  Best (Lowest): FL with Weight Updates ({min(ram_usage):.2f} MB)")

print("\n3. PREDICTION DURATION (seconds)")
print("-" * 60)
for method, dur in zip(methods, duration):
    print(f"  {method:25s}: {dur:6.4f}s")
print(f"  Best (Fastest): FL (Local) ({min(duration):.4f}s)")

print("\n4. MODEL ACCURACY (%)")
print("-" * 60)
for model, acc in zip(models, accuracy):
    print(f"  {model:25s}: {acc:6.2f}%")
print(f"  Best: Deep Learning ({max(accuracy):.2f}%)")

print("\n5. TRAINING DURATION")
print("-" * 60)
for model, dur, label in zip(models, training_duration_seconds, training_labels):
    print(f"  {model:25s}: {label:15s} ({dur}s)")
print(f"  Best (Fastest): Deep Learning (32s)")

print("\n" + "="*60)
print("All visualizations have been saved successfully!")
print("="*60)
