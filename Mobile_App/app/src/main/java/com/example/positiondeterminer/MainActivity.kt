package com.example.positiondeterminer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.positiondeterminer.data.StorageService
import com.example.positiondeterminer.ui.screens.*
import com.example.positiondeterminer.ui.theme.PositionDeterminerTheme
import com.example.positiondeterminer.utils.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var localeManager: LocaleManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        localeManager = LocaleManager(this)
        
        // Apply saved language
        runBlocking {
            val savedLanguage = localeManager.currentLanguage.first()
            localeManager.updateLocale(savedLanguage)
        }
        
        enableEdgeToEdge()
        setContent {
            PositionDeterminerTheme {
                MainScreen(localeManager)
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val localeManager = LocaleManager(newBase)
            val savedLanguage = runBlocking { localeManager.currentLanguage.first() }
            val context = localeManager.updateLocale(savedLanguage)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(localeManager: LocaleManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Observe current language
    val currentLanguage by localeManager.currentLanguage.collectAsState(initial = LocaleManager.LANGUAGE_ENGLISH)
    
    // Check if we're on a detail screen
    val isDetailScreen = currentDestination?.route?.startsWith("history_detail/") == true
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Hide bottom bar on detail screens
            if (!isDetailScreen) {
                ModernFloatingNavBar(
                    screens = listOf(
                        Screen.FederatedLearning,
                        Screen.DeepLearning,
                        Screen.History,
                        Screen.ModelInfo
                    ),
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.FederatedLearning.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.FederatedLearning.route) {
                FederatedLearningScreen()
            }
            composable(Screen.DeepLearning.route) {
                DeepLearningScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetail = { predictionId ->
                        navController.navigate("history_detail/$predictionId")
                    }
                )
            }
            composable(Screen.ModelInfo.route) {
                ModelInfoScreen(
                    onLanguageSelected = { languageCode ->
                        scope.launch {
                            localeManager.setLanguage(languageCode)
                            // Recreate activity to apply new locale
                            (context as? ComponentActivity)?.recreate()
                        }
                    },
                    currentLanguage = currentLanguage
                )
            }
            composable(
                route = "history_detail/{predictionId}",
                arguments = listOf(navArgument("predictionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val predictionId = backStackEntry.arguments?.getString("predictionId")
                if (predictionId != null) {
                    // Get the prediction result from storage
                    val storageService = StorageService(navController.context)
                    val history = runBlocking { storageService.getHistory().first() }
                    val result = history.find { it.id == predictionId }
                    
                    if (result != null) {
                        HistoryDetailScreen(
                            result = result,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object FederatedLearning : Screen("fl", R.string.nav_federated_learning, Icons.Default.AccountTree)
    object DeepLearning : Screen("dl", R.string.nav_deep_learning, Icons.Default.Psychology)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object ModelInfo : Screen("info", R.string.nav_model_info, Icons.Default.Info)
}

@Composable
fun ModernFloatingNavBar(
    screens: List<Screen>,
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    // Floating nav bar with solid, visible background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    NavBarItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}

@Composable
fun NavBarItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .width(70.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with gradient
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label - always visible
        Text(
            text = stringResource(screen.titleRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
