package com.example.positiondeterminer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.positiondeterminer.R
import com.example.positiondeterminer.data.ApiService
import com.example.positiondeterminer.ui.utils.ActivityTranslator
import com.example.positiondeterminer.viewmodel.FederatedLearningUiState
import com.example.positiondeterminer.viewmodel.FederatedLearningViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederatedLearningScreen(viewModel: FederatedLearningViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val isModelReady by viewModel.isModelReady.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    var showActivityDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.fl_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                // Privacy info card with model status
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isModelReady) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isModelReady) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isModelReady) MaterialTheme.colorScheme.tertiary 
                                           else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isModelReady) stringResource(R.string.model_ready) else stringResource(R.string.model_required),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isModelReady) stringResource(R.string.pytorch_mobile_on_device) 
                                           else stringResource(R.string.download_to_enable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Download button or status badges
                        if (!isModelReady) {
                            Button(
                                onClick = { viewModel.downloadPyTorchModel() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.downloading))
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.download_model_size))
                                }
                            }
                        } else {
                            // Privacy features
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = stringResource(R.string.encrypted),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.OfflineBolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = stringResource(R.string.offline),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Re-download button (smaller)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.downloadPyTorchModel() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDownloading,
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.update_model), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                // Spacer to push content towards center for non-Success states
                if (uiState !is FederatedLearningUiState.Success) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Main content
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "fl_state"
                ) { state ->
                    when (state) {
                        is FederatedLearningUiState.InitializingModel -> {
                            FLLoadingState(stringResource(R.string.initializing_pytorch), Icons.Default.Download)
                        }
                        
                        is FederatedLearningUiState.Idle -> {
                            FLIdleState(
                                isModelReady = isModelReady,
                                onStartClick = { viewModel.startPrediction() },
                                onStartFullFlowClick = { viewModel.startFullFLFlow() }
                            )
                        }
                        
                        is FederatedLearningUiState.Collecting -> {
                            FLLoadingState(stringResource(R.string.collecting_sensor_data), Icons.Default.Sensors)
                        }
                        
                        is FederatedLearningUiState.Predicting -> {
                            FLLoadingState(stringResource(R.string.making_prediction), Icons.Default.Psychology)
                        }
                        
                        is FederatedLearningUiState.Success -> {
                            FLSuccessState(
                                prediction = state.prediction,
                                confidence = state.confidence,
                                deviceMetrics = state.deviceMetrics,
                                apiMetrics = state.apiMetrics,
                                allProbabilities = state.allProbabilities,
                                showFeedback = state.showFeedback,
                                isOnDevice = state.isOnDevice,
                                fullFlowMetrics = state.fullFlowMetrics,
                                onConfirm = { viewModel.confirmPrediction() },
                                onCorrect = { showActivityDialog = true },
                                onReset = { viewModel.reset() },
                                onNewPrediction = { viewModel.reset() }
                            )
                        }
                        
                        is FederatedLearningUiState.Training -> {
                            FLLoadingState(stringResource(R.string.computing_gradients), Icons.Default.ModelTraining)
                        }
                        
                        is FederatedLearningUiState.TrainingSuccess -> {
                            FLTrainingSuccessState(
                                message = state.message,
                                onReset = { viewModel.reset() }
                            )
                        }
                        
                        is FederatedLearningUiState.Error -> {
                            FLErrorState(
                                message = state.message,
                                onRetry = { viewModel.reset() }
                            )
                        }
                    }
                }
                
                // Spacer to push content towards center for non-Success states
                if (uiState !is FederatedLearningUiState.Success) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                }
            }
        }
    }
    
    // Activity selection dialog
    if (showActivityDialog) {
        AlertDialog(
            onDismissRequest = { showActivityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_correct_activity_title))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiService.ACTIVITY_LABELS.forEachIndexed { index, activity ->
                        FilledTonalButton(
                            onClick = {
                                viewModel.updateWeights(index, activity)
                                showActivityDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(activity)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActivityDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun FLIdleState(isModelReady: Boolean, onStartClick: () -> Unit, onStartFullFlowClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Text(
            text = if (isModelReady) stringResource(R.string.ready_to_learn) else stringResource(R.string.model_required),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (isModelReady) stringResource(R.string.start_prediction_help)
                   else stringResource(R.string.download_model_first),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Local FL button (existing)
        Button(
            onClick = onStartClick,
            enabled = isModelReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.local_fl_prediction), style = MaterialTheme.typography.titleMedium)
        }
        
        // NEW: Full FL Flow button
        OutlinedButton(
            onClick = onStartFullFlowClick,
            enabled = isModelReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.full_fl_flow_with_metrics), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FLLoadingState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FLSuccessState(
    prediction: String,
    confidence: Double,
    deviceMetrics: com.example.positiondeterminer.data.DeviceMetrics,
    apiMetrics: com.example.positiondeterminer.data.ApiMetrics?,
    allProbabilities: Map<String, Double>,
    showFeedback: Boolean,
    isOnDevice: Boolean = true,
    fullFlowMetrics: com.example.positiondeterminer.data.FullFlowMetrics? = null,
    onConfirm: () -> Unit,
    onCorrect: () -> Unit,
    onReset: () -> Unit,
    onNewPrediction: () -> Unit
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // On-Device Badge
                if (isOnDevice) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.on_device_prediction),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Text(
                    text = ActivityTranslator.translate(prediction, context),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Confidence
                FLMetricChip(
                    label = stringResource(R.string.confidence),
                    value = "${(confidence * 100).toInt()}%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Device Metrics Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.device_metrics),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FLMetricChip(
                        label = stringResource(R.string.duration),
                        value = String.format(Locale.getDefault(),"%.2fs", deviceMetrics.duration_seconds),
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    FLMetricChip(
                        label = stringResource(R.string.cpu),
                        value = String.format(Locale.getDefault(),"%.1f%%", deviceMetrics.cpu_usage_percent),
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FLMetricChip(
                        label = stringResource(R.string.ram),
                        value = String.format(Locale.getDefault(),"%.1f MB", deviceMetrics.ram_usage_mb),
                        icon = Icons.Default.Storage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // API Metrics Card
        apiMetrics?.let { metrics ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.api_metrics),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FLMetricChip(
                            label = stringResource(R.string.duration),
                            value = String.format(Locale.getDefault(),"%.2fs", metrics.duration_seconds),
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FLMetricChip(
                            label = stringResource(R.string.cpu),
                            value = String.format(Locale.getDefault(),"%.1f%%", metrics.cpu_usage_percent),
                            icon = Icons.Default.Memory,
                            modifier = Modifier.weight(1f)
                        )
                        FLMetricChip(
                            label = stringResource(R.string.ram),
                            value = String.format(Locale.getDefault(),"%.1f MB", metrics.ram_usage_mb),
                            icon = Icons.Default.Storage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Full FL Flow Metrics Card
        fullFlowMetrics?.let { metrics ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.full_fl_flow_timing),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FLMetricChip(
                            label = stringResource(R.string.prediction),
                            value = "${metrics.prediction_time_ms}ms",
                            icon = Icons.Default.Psychology,
                            modifier = Modifier.weight(1f)
                        )
                        FLMetricChip(
                            label = stringResource(R.string.gradients),
                            value = "${metrics.gradient_calc_time_ms}ms",
                            icon = Icons.Default.Calculate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FLMetricChip(
                            label = stringResource(R.string.api_send),
                            value = "${metrics.api_send_time_ms}ms",
                            icon = Icons.Default.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )
                        FLMetricChip(
                            label = stringResource(R.string.model_update),
                            value = "${metrics.model_update_time_ms}ms",
                            icon = Icons.Default.Update,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FLMetricChip(
                        label = stringResource(R.string.total_time),
                        value = "${metrics.total_time_ms}ms",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    metrics.gradient_norm?.let { norm ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.gradient_norm)}: ${String.format(Locale.getDefault(),"%.4f", norm)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // All Probabilities Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.all_probabilities),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sort by probability descending
                allProbabilities.entries.sortedByDescending { it.value }.forEach { (activity, prob) ->
                    FLProbabilityBar(
                        activity = activity,
                        probability = prob,
                        isTopPrediction = activity == prediction
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        if (showFeedback) {
            Text(
                text = stringResource(R.string.is_prediction_correct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.correct))
                }
                
                OutlinedButton(
                    onClick = onCorrect,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.wrong))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // New Prediction button (skip training, just make another prediction)
            OutlinedButton(
                onClick = onNewPrediction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_prediction_skip), style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_prediction), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FLTrainingSuccessState(message: String, onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
        }
        
        Text(
            text = stringResource(R.string.training_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(stringResource(R.string.new_prediction), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FLErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Text(
            text = stringResource(R.string.oops),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.try_again), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FLMetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FLProbabilityBar(
    activity: String,
    probability: Double,
    isTopPrediction: Boolean
) {
    val context = LocalContext.current
    val barColor = if (isTopPrediction) 
        MaterialTheme.colorScheme.tertiary 
    else 
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTopPrediction) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = ActivityTranslator.translate(activity, context),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isTopPrediction) FontWeight.Bold else FontWeight.Normal
                )
            }
            Text(
                text = String.format(Locale.getDefault(),"%.1f%%", probability),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = { (probability / 100.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.2f),
        )
    }
}
