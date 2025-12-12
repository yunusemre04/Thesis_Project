package com.example.positiondeterminer.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.positiondeterminer.ui.theme.AppGradient
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.positiondeterminer.R
import com.example.positiondeterminer.data.PredictionResult
import com.example.positiondeterminer.ui.utils.ActivityTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    result: PredictionResult,
    onBack: () -> Unit
) {

    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault()) }
    val date = remember(result.timestamp) { Date(result.timestamp) }
    
    val isFullFL = result.type == "Full FL"
    val primaryColor = if (result.type == "FL" || isFullFL)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.primary
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )

        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.prediction_details)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    HeaderCard(result, date, dateFormatter, primaryColor)
                }
                
                // Device Metrics Card
                item {
                    result.deviceMetrics?.let { metrics ->
                        MetricsCard(
                            title = stringResource(R.string.device_metrics),
                            subtitle = stringResource(R.string.device_metrics_subtitle),
                            icon = Icons.Default.PhoneAndroid,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            metrics = metrics
                        )
                    }
                }
                
                // Full FL Flow Metrics Card (only for Full FL)
                item {
                    if (isFullFL && result.fullFlowMetrics != null) {
                        FullFLMetricsCard(
                            metrics = result.fullFlowMetrics,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                }
                
                // API Metrics Card
                item {
                    result.apiMetrics?.let { metrics ->
                        ApiMetricsCard(
                            title = stringResource(R.string.api_metrics),
                            subtitle = stringResource(R.string.api_metrics_subtitle),
                            icon = Icons.Default.CloudQueue,
                            color = if (result.type == "FL" || isFullFL)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            metrics = metrics
                        )
                    }
                }
                
                // All Probabilities Card
                item {
                    if (!result.allProbabilities.isNullOrEmpty()) {
                        AllProbabilitiesCard(
                            probabilities = result.allProbabilities,
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    result: PredictionResult,
    date: Date,
    dateFormatter: SimpleDateFormat,
    primaryColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = primaryColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon with subtle gradient circle for a modern look
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (result.type == "FL" || result.type == "Full FL") Icons.Default.AccountTree else Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // Activity
            Text(
                text = ActivityTranslator.translate(result.activity, context = LocalContext.current),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Confidence
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = primaryColor
                    )
                    Text(
                        text = "${(result.confidence * 100).toInt()}% Confidence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
            
            // Model Type
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (result.type == "Full FL") {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = when (result.type) {
                            "Full FL" -> stringResource(R.string.full_fl)
                            "FL" -> stringResource(R.string.fl_title)
                            else -> stringResource(R.string.dl_title)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.format(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    metrics: com.example.positiondeterminer.data.DeviceMetrics
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailMetricItem(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.duration),
                        value = String.format(Locale.getDefault(),"%.2f s", metrics.duration_seconds),
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricItem(
                        icon = Icons.Default.Memory,
                        label = stringResource(R.string.cpu),
                        value = String.format(Locale.getDefault(),"%.1f%%", metrics.cpu_usage_percent),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                DetailMetricItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(R.string.ram),
                    value = String.format(Locale.getDefault(),"%.1f MB", metrics.ram_usage_mb),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ApiMetricsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    metrics: com.example.positiondeterminer.data.ApiMetrics
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailMetricItem(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.duration),
                        value = String.format(Locale.getDefault(),"%.2f s", metrics.duration_seconds),
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricItem(
                        icon = Icons.Default.Memory,
                        label = stringResource(R.string.cpu),
                        value = String.format(Locale.getDefault(),"%.1f%%", metrics.cpu_usage_percent),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                DetailMetricItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(R.string.ram),
                    value = String.format(Locale.getDefault(),"%.1f MB", metrics.ram_usage_mb),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
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
private fun AllProbabilitiesCard(
    probabilities: Map<String, Double>,
    primaryColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = primaryColor
                )
                Text(
                    text = stringResource(R.string.all_probabilities),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sort probabilities by value descending
            val sortedProbabilities = probabilities.entries.sortedByDescending { it.value }
            
            sortedProbabilities.forEachIndexed { index, (activity, probability) ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                DetailProbabilityBar(activity, probability, primaryColor)
            }
        }
    }
}

@Composable
private fun DetailProbabilityBar(
    activity: String,
    probability: Double,
    primaryColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activity,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format(Locale.getDefault(),"%.1f%%", probability),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (probability / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = primaryColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun FullFLMetricsCard(
    metrics: com.example.positiondeterminer.data.FullFlowMetrics,
    color: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(R.string.full_fl_flow_timing),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.complete_fl_cycle_breakdown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Timing metrics grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimingMetricItem(
                        label = stringResource(R.string.prediction),
                        value = "${metrics.prediction_time_ms}ms",
                        icon = Icons.Default.Psychology,
                        modifier = Modifier.weight(1f)
                    )
                    TimingMetricItem(
                        label = stringResource(R.string.gradients),
                        value = "${metrics.gradient_calc_time_ms}ms",
                        icon = Icons.Default.Calculate,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimingMetricItem(
                        label = stringResource(R.string.api_send),
                        value = "${metrics.api_send_time_ms}ms",
                        icon = Icons.Default.CloudUpload,
                        modifier = Modifier.weight(1f)
                    )
                    TimingMetricItem(
                        label = stringResource(R.string.model_update),
                        value = "${metrics.model_update_time_ms}ms",
                        icon = Icons.Default.Update,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                TimingMetricItem(
                    label = stringResource(R.string.total_time),
                    value = "${metrics.total_time_ms}ms",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.fillMaxWidth(),
                    isTotal = true
                )
                
                metrics.gradient_norm?.let { norm ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.gradient_norm),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.4f", norm),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isTotal: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isTotal) 
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        else 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isTotal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTotal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
