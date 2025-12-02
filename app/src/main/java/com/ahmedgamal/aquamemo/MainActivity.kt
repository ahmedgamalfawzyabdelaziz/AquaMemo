package com.ahmedgamal.aquamemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ahmedgamal.aquamemo.ui.CandleDataScreen
import com.ahmedgamal.aquamemo.ui.DataDisplayScreen
import com.ahmedgamal.aquamemo.ui.HomeScreen
import com.ahmedgamal.aquamemo.ui.MainScreen
import com.ahmedgamal.aquamemo.ui.SettingsScreen
import com.ahmedgamal.aquamemo.ui.SplashScreen
import com.ahmedgamal.aquamemo.ui.theme.AquaMemoTheme
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.content.edit
import com.ahmedgamal.aquamemo.ui.CandlePricesScreen
import com.ahmedgamal.aquamemo.ui.CustomIntervalsScreen
import com.ahmedgamal.aquamemo.ui.NotificationScreen
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import com.ahmedgamal.aquamemo.ui.TdsTrackerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var filterRepository: FilterRepository
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private val mainViewModel: MainViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showToast(getString(R.string.notification_permission_granted))
            rescheduleAllNotifications()
        } else {
            showToast(getString(R.string.notification_permission_denied))
        }
        initializeAppContent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ⬇️ حل إجباري للـ Release builds - يطبق قبل أي شيء
        applyLanguageFix()

        // تطبيق اللغة
        applyLanguageForCustomDevices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                initializeAppContent()
            }
        } else {
            initializeAppContent()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun attachBaseContext(newBase: Context) {
        // تطبيق اللغة قبل إنشاء الـ Activity
        val sharedPref = newBase.getSharedPreferences("AppSettings", MODE_PRIVATE)
        val languageCode = sharedPref.getString("language", "en") ?: "en"

        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun applyLanguageFix() {
        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)

        Log.d("LanguageDebug", "=== LANGUAGE FIX START ===")

        // تأكد أن الإنجليزية هي الـ Default
        if (!sharedPref.contains("language")) {
            sharedPref.edit { putString("language", "en") }
        }
        val currentLang = sharedPref.getString("language", "en") ?: "en"

        if (currentLang != "en" && currentLang != "ar") {
            Log.d("LanguageDebug", "Invalid language detected - Fixing to English")
            sharedPref.edit { putString("language", "en") }
        }

        sharedPref.getString("language", "error") ?: "error"
    }
    private fun applyLanguageForCustomDevices() {
        if (Build.MANUFACTURER.equals("realme", ignoreCase = true) ||
            Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("oppo", ignoreCase = true)) {

            Log.d("LanguageDebug", "Applying special fix for: ${Build.MANUFACTURER}")

            val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
            val languageCode = sharedPref.getString("language", "en") ?: "en"
            val locale = Locale.forLanguageTag(languageCode)

            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)

            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }

    private fun initializeAppContent() {
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val fontSize by settingsViewModel.fontSize.collectAsState(initial = "medium")
            val themePreference by settingsViewModel.themePreference.collectAsState(initial = "system")

            val gradientColors = remember {
                listOf(
                    Color(0xFFE3F2FD),
                    Color(0xFFBBDEFB),
                    Color(0xFF90CAF9)
                )
            }

            AquaMemoTheme(
                themePreference = themePreference,
                fontSize = fontSize
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(colors = gradientColors)
                            )
                    ) {
                        val navController = rememberNavController()
                        var showSplashScreen by remember { mutableStateOf(true) }
                        var startDestination by remember { mutableStateOf("home_screen") }
                        var isLoading by remember { mutableStateOf(true) }
                        val hideSplashScreen = {
                            showSplashScreen = false
                        }

                        LaunchedEffect(key1 = true) {
                            delay(1000)

                            val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
                            val hasDataStored = sharedPref.getBoolean("has_data", false)

                            val hasData = if (hasDataStored) {
                                true
                            } else {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        mainViewModel.allFilters.first().isNotEmpty()
                                    }
                                    if (result) {
                                        sharedPref.edit { putBoolean("has_data", true) }
                                    }
                                    result
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            startDestination = if (hasData) "home_screen" else "main_screen"
                            isLoading = false

                        }

                        if (showSplashScreen) {
                            // ✅ 3. استخدام SplashScreen مع دالة التخطي الجديدة
                            SplashScreen(
                                isVisible = true,
                                onSkipClicked = hideSplashScreen,
                                onVideoEnd = hideSplashScreen,// ⬅️ تمرير دالة الإخفاء
                                fontSize = fontSize
                            )
                        } else if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = getString(R.string.loading_data),
                                    modifier = Modifier.padding(top = 16.dp),
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                composable("main_screen") {
                                    MainScreen(
                                        onNavigateToInput = { filterType ->
                                            try {
                                                navController.navigate("candle_data_screen/$filterType") {
                                                    launchSingleTop = true
                                                    popUpTo("main_screen") { saveState = true }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    getString(R.string.billing_flow_failed, e.message ?: ""),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onNavigateToSettings = {
                                            navController.navigate("settings_screen") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable("home_screen") {
                                    HomeScreen(
                                        onNavigateToDataDisplay = {
                                            navController.navigate("data_display_screen") {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onNavigateToSettings = {
                                            navController.navigate("settings_screen") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToNotifications = {
                                            navController.navigate("notification_screen") {
                                                launchSingleTop = true
                                            }
                                        },
                                                onNavigateToTdsTracker = {
                                            navController.navigate("tds_tracker_screen") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable(
                                    "candle_data_screen/{filterType}",
                                    arguments = listOf(
                                        navArgument("filterType") {
                                            type = NavType.StringType
                                        }
                                    )
                                ) { backStackEntry ->
                                    val filterType by remember {
                                        derivedStateOf {
                                            backStackEntry.arguments?.getString("filterType") ?: ""
                                        }
                                    }

                                    LaunchedEffect(filterType) {
                                    }

                                    val numberOfCandles = when (filterType) {
                                        "3 Stages" -> 3
                                        "5 Stages" -> 5
                                        "7 Stages" -> 7
                                        else -> 0
                                    }

                                    if (numberOfCandles > 0) {
                                        CandleDataScreen(
                                            onNavigateToDisplay = {
                                                navController.navigate("home_screen") {
                                                    popUpTo(0) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            },
                                            filterType = filterType,
                                            numberOfCandles = numberOfCandles
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(getString(R.string.billing_product_not_found, filterType))
                                        }
                                    }
                                }

                                composable("data_display_screen") {
                                    DataDisplayScreen(
                                        onNavigateToEdit = { filterType, candleNumber ->
                                            navController.navigate("candle_data_screen/$filterType?candleNumberToEdit=$candleNumber") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToSettings = {
                                            navController.navigate("settings_screen") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToNotifications = {
                                            navController.navigate("notification_screen") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable("settings_screen") {
                                    SettingsScreen(onNavigateToMainScreen = {
                                        navController.navigate("main_screen") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },navController = navController
                                    )
                                }
                                composable("candle_prices_screen") {
                                    CandlePricesScreen(
                                        onBackClick = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable("custom_intervals_screen") {
                                    CustomIntervalsScreen(
                                        onBackClick = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable("notification_screen") {
                                    NotificationScreen(
                                        onBackClick = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable("tds_tracker_screen") {
                                    val context = LocalContext.current
                                    TdsTrackerScreen(
                                        onBackClick = {
                                            navController.popBackStack()
                                        },
                                        onNavigateToSubscription = {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.subscribe_instruction),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun rescheduleAllNotifications() {
        mainViewModel.rescheduleAllNotifications()
    }
    private fun handleNotificationIntent(intent: Intent?) {
        val extras = intent?.extras ?: return

        // التحقق من وجود البيانات التي أرسلناها
        if (extras.containsKey("title") && extras.containsKey("message")) {
            val title = extras.getString("title")
            val message = extras.getString("message")

            if (title != null && message != null) {
                Log.d("MainActivity", "Handling notification data from intent.")

                // إنشاء كائن الإشعار
                val notificationHistory = NotificationHistory(
                    type = "REMOTE_ADMIN",
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    iconType = "INFO"
                )

                // حفظه في قاعدة البيانات في Coroutine
                activityScope.launch {
                    try {
                        filterRepository.insertNotification(notificationHistory)

                        // (هام) حذف البيانات من الـ Intent
                        intent.removeExtra("title")
                        intent.removeExtra("message")

                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to save remote message from intent", e)
                    }
                }
            }
        }
    }
}