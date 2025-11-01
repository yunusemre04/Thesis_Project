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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.positiondeterminer.R
import com.example.positiondeterminer.data.ApiService
import com.example.positiondeterminer.data.ModelInfoResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInfoScreen(
    onLanguageSelected: (String) -> Unit = {},
    currentLanguage: String = "en"
) {
    var dlModelInfo by remember { mutableStateOf<ModelInfoResponse?>(null) }
    var flModelInfo by remember { mutableStateOf<ModelInfoResponse?>(null) }
    var pytorchModelInfo by remember { mutableStateOf<com.example.positiondeterminer.data.PyTorchModelInfoResponse?>(null) }
    var dlEvaluation by remember { mutableStateOf<com.example.positiondeterminer.data.EvaluationResults?>(null) }
    var flEvaluation by remember { mutableStateOf<com.example.positiondeterminer.data.EvaluationResults?>(null) }
    var pytorchEvaluation by remember { mutableStateOf<com.example.positiondeterminer.data.EvaluationResults?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                val dlResponse = ApiService.api.dlInfo()
                val flResponse = ApiService.api.flInfo()
                val pytorchResponse = ApiService.api.pytorchModelInfo()
                val dlEvalResponse = ApiService.api.dlEvaluate()
                val flEvalResponse = ApiService.api.flEvaluate()
                val pytorchEvalResponse = ApiService.api.pytorchEvaluate()
                
                if (dlResponse.isSuccessful && flResponse.isSuccessful) {
                    val dlBody = dlResponse.body()
                    val flBody = flResponse.body()
                    
                    if (dlBody?.success == true) {
                        dlModelInfo = dlBody.model_info
                    }
                    if (flBody?.success == true) {
                        flModelInfo = flBody.model_info
                    }
                    
                    if (pytorchResponse.isSuccessful) {
                        val pytorchBody = pytorchResponse.body()
                        if (pytorchBody?.success == true) {
                            pytorchModelInfo = pytorchBody
                        }
                    }
                    
                    if (dlEvalResponse.isSuccessful) {
                        val dlEvalBody = dlEvalResponse.body()
                        if (dlEvalBody?.success == true) {
                            dlEvaluation = dlEvalBody.evaluation
                        }
                    }
                    
                    if (flEvalResponse.isSuccessful) {
                        val flEvalBody = flEvalResponse.body()
                        if (flEvalBody?.success == true) {
                            flEvaluation = flEvalBody.evaluation
                        }
                    }
                    
                    if (pytorchEvalResponse.isSuccessful) {
                        val pytorchEvalBody = pytorchEvalResponse.body()
                        if (pytorchEvalBody?.success == true) {
                            pytorchEvaluation = pytorchEvalBody.evaluation
                        }
                    }
                    
                    if (dlModelInfo == null && flModelInfo == null && pytorchModelInfo == null) {
                        error = context.getString(R.string.failed_to_load)
                    }
                } else {
                    error = "${context.getString(R.string.failed_to_load)}: ${dlResponse.message()}"
                }
            } catch (e: Exception) {
                error = e.message ?: context.getString(R.string.failed_to_load)
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
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
                                Icons.Default.ModelTraining,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.model_info_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showLanguageDialog = true }) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Language",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            if (isLoading) {
                LoadingState(modifier = Modifier.padding(padding))
            } else if (error != null) {
                ErrorState(error = error!!, onRetry = {
                    scope.launch {
                        try {
                            isLoading = true
                            error = null
                            val dlResponse = ApiService.api.dlInfo()
                            val flResponse = ApiService.api.flInfo()
                            val dlEvalResponse = ApiService.api.dlEvaluate()
                            val flEvalResponse = ApiService.api.flEvaluate()
                            
                            if (dlResponse.isSuccessful && flResponse.isSuccessful) {
                                val dlBody = dlResponse.body()
                                val flBody = flResponse.body()
                                
                                if (dlBody?.success == true) {
                                    dlModelInfo = dlBody.model_info
                                }
                                if (flBody?.success == true) {
                                    flModelInfo = flBody.model_info
                                }
                                
                                if (dlEvalResponse.isSuccessful) {
                                    val dlEvalBody = dlEvalResponse.body()
                                    if (dlEvalBody?.success == true) {
                                        dlEvaluation = dlEvalBody.evaluation
                                    }
                                }
                                
                                if (flEvalResponse.isSuccessful) {
                                    val flEvalBody = flEvalResponse.body()
                                    if (flEvalBody?.success == true) {
                                        flEvaluation = flEvalBody.evaluation
                                    }
                                }
                                
                                if (dlModelInfo == null && flModelInfo == null) {
                                    error = "Failed to load model info"
                                }
                            } else {
                                error = "Failed to load model info: ${dlResponse.message()}"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to load model info"
                        } finally {
                            isLoading = false
                        }
                    }
                }, modifier = Modifier.padding(padding))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Modern Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when (selectedTab) {
                                            0 -> MaterialTheme.colorScheme.primary
                                            1 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.deep_learning),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.federated_learning),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    stringResource(R.string.pytorch_mobile),
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    // Tab Content
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith 
                            fadeOut(animationSpec = tween(300))
                        },
                        label = "tab content"
                    ) { tab ->
                        when (tab) {
                            0 -> dlModelInfo?.let { info ->
                                ModelTabContent(
                                    modelInfo = info,
                                    evaluation = dlEvaluation,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                            1 -> flModelInfo?.let { info ->
                                ModelTabContent(
                                    modelInfo = info,
                                    evaluation = flEvaluation,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    accentColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            2 -> pytorchModelInfo?.let { info ->
                                PyTorchModelTabContent(
                                    pytorchInfo = info,
                                    evaluation = pytorchEvaluation,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    accentColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    LanguageOption(
                        language = stringResource(R.string.english),
                        isSelected = currentLanguage == "en",
                        onClick = {
                            onLanguageSelected("en")
                            showLanguageDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LanguageOption(
                        language = stringResource(R.string.turkish),
                        isSelected = currentLanguage == "tr",
                        onClick = {
                            onLanguageSelected("tr")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun LanguageOption(
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ModelTraining,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.loading_model_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(R.string.error_loading_info),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ModelTabContent(
    modelInfo: ModelInfoResponse,
    evaluation: com.example.positiondeterminer.data.EvaluationResults?,
    containerColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model Architecture Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Architecture,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.architecture),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                modelInfo.model_type?.let { modelType ->
                    ModernInfoRow(Icons.Default.Category, stringResource(R.string.model_type), modelType, accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                modelInfo.input_shape?.let { inputShape ->
                    ModernInfoRow(Icons.Default.Input, stringResource(R.string.input_shape), inputShape.joinToString(" × "), accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                modelInfo.output_shape?.let { outputShape ->
                    ModernInfoRow(Icons.Default.Output, stringResource(R.string.output_shape), outputShape.joinToString(" × "), accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                modelInfo.num_layers?.let { layers ->
                    ModernInfoRow(Icons.Default.Layers, stringResource(R.string.layers), layers.toString(), accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                modelInfo.num_parameters?.let { params ->
                    ModernInfoRow(Icons.Default.Functions, stringResource(R.string.parameters), String.format("%,d", params), accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                modelInfo.model_size_kb?.let { size ->
                    ModernInfoRow(Icons.Default.Storage, stringResource(R.string.model_size), String.format("%.2f KB", size), accentColor)
                }
            }
        }
        
        // Test Results Card
        evaluation?.let { eval ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.test_results),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${eval.test_samples} samples",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Accuracy with progress bar
                    AccuracyMetric(
                        label = "Accuracy",
                        value = eval.overall_accuracy,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    eval.precision_weighted?.let { precision ->
                        AccuracyMetric(stringResource(R.string.precision), precision, Color(0xFF2196F3))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    eval.recall_weighted?.let { recall ->
                        AccuracyMetric(stringResource(R.string.recall), recall, Color(0xFFFF9800))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    eval.f1_score_weighted?.let { f1 ->
                        AccuracyMetric("F1-Score", f1, Color(0xFF9C27B0))
                    }
                }
            }
        }
        
        // Activities Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.activities),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ApiService.ACTIVITY_LABELS.size} recognized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ApiService.ACTIVITY_LABELS.forEachIndexed { index, activity ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = activity,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    if (index < ApiService.ACTIVITY_LABELS.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = iconColor
        )
    }
}

@Composable
private fun AccuracyMetric(label: String, value: Double, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%.2f%%", value * 100),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = value.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun PyTorchModelTabContent(
    pytorchInfo: com.example.positiondeterminer.data.PyTorchModelInfoResponse,
    evaluation: com.example.positiondeterminer.data.EvaluationResults?,
    containerColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📱 ${pytorchInfo.model_format ?: "PyTorch Mobile"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                
                HorizontalDivider(color = accentColor.copy(alpha = 0.3f))
                
                ModernInfoRow(Icons.Default.InsertDriveFile, stringResource(R.string.file), pytorchInfo.file_name ?: "fl_model.pt", accentColor)
                ModernInfoRow(Icons.Default.Storage, stringResource(R.string.size), "${pytorchInfo.file_size_mb ?: 0.0} MB", accentColor)
                
                pytorchInfo.architecture?.let { arch ->
                    ModernInfoRow(Icons.Default.Input, stringResource(R.string.input), "${arch.input_size} ${stringResource(R.string.features_unit)}", accentColor)
                    ModernInfoRow(Icons.Default.Output, stringResource(R.string.output), "${arch.output_classes} ${stringResource(R.string.classes_unit)}", accentColor)
                    ModernInfoRow(Icons.Default.ShowChart, stringResource(R.string.activation), arch.activation ?: "ReLU", accentColor)
                    ModernInfoRow(Icons.Default.Numbers, stringResource(R.string.parameters), arch.total_parameters ?: "~500K", accentColor)
                }
            }
        }
        // Evaluation Results Card
        evaluation?.let { eval ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "📊 Model Evaluation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    HorizontalDivider(color = accentColor.copy(alpha = 0.3f))

                    // Overall Accuracy
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.overall_accuracy),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                String.format("%.2f%%", eval.overall_accuracy * 100),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                "${eval.test_samples} test samples",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Weighted Metrics
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.weighted_metrics),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    eval.precision_weighted?.let { precision ->
                        AccuracyMetric(stringResource(R.string.precision), precision, accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    eval.recall_weighted?.let { recall ->
                        AccuracyMetric(stringResource(R.string.recall), recall, accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    eval.f1_score_weighted?.let { f1 ->
                        AccuracyMetric("F1-Score", f1, accentColor)
                    }
                }
            }
        }

        // Federated Learning Status Card
        pytorchInfo.federated_learning?.let { fl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (fl.enabled) Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else containerColor.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔄 Federated Learning",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (fl.enabled) Color(0xFF4CAF50) else accentColor
                        )
                        if (fl.enabled) {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = accentColor.copy(alpha = 0.3f))
                    
                    ModernInfoRow(Icons.Default.CheckCircle, stringResource(R.string.model_status), if (fl.model_loaded) "✅ ${stringResource(R.string.loaded)}" else "❌ ${stringResource(R.string.not_loaded)}", 
                        if (fl.model_loaded) Color(0xFF4CAF50) else Color(0xFFF44336))
                    ModernInfoRow(Icons.Default.Replay, stringResource(R.string.current_round), "#${fl.current_round}", accentColor)
                    ModernInfoRow(Icons.Default.PendingActions, stringResource(R.string.pending_gradients), "${fl.pending_gradients} ${stringResource(R.string.devices_unit)}", accentColor)
                    ModernInfoRow(Icons.Default.TrendingUp, stringResource(R.string.learning_rate), fl.learning_rate.toString(), accentColor)
                    ModernInfoRow(Icons.Default.GroupWork, stringResource(R.string.aggregation), fl.aggregation_method ?: "FedAvg", accentColor)
                    
                    // Privacy Guarantees
                    if (!fl.privacy_guarantees.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "🔒 Privacy Guarantees",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        fl.privacy_guarantees.forEach { guarantee ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    guarantee,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    // Contributing Devices
                    if (fl.devices_contributing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "📱 Contributing Devices (${fl.devices_contributing.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        fl.devices_contributing.take(5).forEach { deviceId ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = accentColor
                                )
                                Text(
                                    deviceId,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        if (fl.devices_contributing.size > 5) {
                            Text(
                                "... and ${fl.devices_contributing.size - 5} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                }
            }
        }
        

        // Features Card
        if (!pytorchInfo.features.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "✨ Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    
                    pytorchInfo.features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
