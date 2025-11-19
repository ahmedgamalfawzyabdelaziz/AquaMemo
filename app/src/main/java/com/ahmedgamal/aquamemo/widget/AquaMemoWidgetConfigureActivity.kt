package com.ahmedgamal.aquamemo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.OneTimeWorkRequestBuilder
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.ui.theme.AquaMemoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.work.WorkManager
import javax.inject.Inject

@AndroidEntryPoint
class AquaMemoWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    @Inject lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            AquaMemoTheme {
                WidgetConfigureScreen(
                    appWidgetId = appWidgetId,
                    workManager = workManager,
                    onSave = {
                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigureScreen(appWidgetId: Int,workManager: WorkManager, onSave: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fontSizeScale by remember { mutableFloatStateOf(1.0f) }
    var backgroundOpacity by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(Unit) {
        val glanceManager = GlanceAppWidgetManager(context)
        // Use the built-in function, requires context from LocalContext.current
        val glanceId = glanceManager.getGlanceIdBy(appWidgetId)

        if (glanceId != null) {
            val prefs = getAppWidgetState(context, AquaMemoWidget.stateDefinition, glanceId)
            fontSizeScale = prefs[WidgetData.fontSizeScaleKey] ?: 1.0f
            backgroundOpacity = prefs[WidgetData.backgroundOpacityKey] ?: 1.0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center the preview
        ) {

            // --- LIVE PREVIEW ---
            Text(
                text = stringResource(R.string.widget_config_preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            WidgetPreview(
                fontSizeScale = fontSizeScale,
                backgroundOpacity = backgroundOpacity
            )

            Spacer(Modifier.height(24.dp))
            // --- END: LIVE PREVIEW ---

            // --- Font Size Setting ---
            Text(
                text = stringResource(R.string.widget_config_font_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth() // Align title left
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    stringResource(R.string.widget_config_small) to 0.8f,
                    stringResource(R.string.widget_config_medium) to 1.0f,
                    stringResource(R.string.widget_config_large) to 1.2f
                ).forEachIndexed { index, (label, value) ->
                    val isSelected = (value == fontSizeScale)
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        2 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { fontSizeScale = value },
                        label = { Text(label) },
                        shape = shape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Opacity Setting ---
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth() // Align title left
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(backgroundOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(48.dp)
                )
                Slider(
                    value = backgroundOpacity,
                    onValueChange = { backgroundOpacity = it },
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))

            // --- Save Button ---
            Button(
                onClick = {
                    coroutineScope.launch {
                        val glanceManager = GlanceAppWidgetManager(context)
                        // Use the built-in function, requires context from LocalContext.current
                        val glanceId = glanceManager.getGlanceIdBy(appWidgetId)

                        if (glanceId != null) {
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs[WidgetData.fontSizeScaleKey] = fontSizeScale
                                prefs[WidgetData.backgroundOpacityKey] = backgroundOpacity
                            }
                            AquaMemoWidget.update(context, glanceId)
                            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                            workManager.enqueue(workRequest)
                        }
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.widget_config_save))
            }
        }
    }
}

@Composable
fun WidgetPreview(
    fontSizeScale: Float,
    backgroundOpacity: Float
) {
    // Get context to access resources directly
    val context = LocalContext.current

    // Use sample data for the preview
    val sampleCandleName = stringResource(R.string.widget_preview_stage_1)
    val sampleDaysRemaining = 92
    val sampleNextDate = "2026-01-25"
    val sampleNextMaintText = stringResource(R.string.next_maintenance_widget)

    // WORKAROUND: Get quantity string directly using context.resources
    val sampleDaysText = context.resources.getQuantityString(
        R.plurals.days_remaining_widget,
        sampleDaysRemaining,
        sampleDaysRemaining // Pass count for formatting arg %d
    )

    // Get colors from the standard MaterialTheme
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColorWithAlpha = surfaceColor.copy(alpha = backgroundOpacity)
    val daysColor = getDaysRemainingColorPreview()
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f) // Make preview slightly smaller than full width
            .aspectRatio(1.6f), // Approximate widget ratio
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColorWithAlpha
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. The Filter Name (Top Title)
            Text(
                text = sampleCandleName,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (18 * fontSizeScale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(12.dp))

            // 2. The Big Number (Main Value)
            Text(
                text = sampleDaysRemaining.toString(),
                style = TextStyle(
                    color = daysColor,
                    fontSize = (60 * fontSizeScale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            // 3. The Label for the number
            Text(
                // Use the string obtained via context.resources
                text = sampleDaysText,
                style = TextStyle(
                    color = daysColor,
                    fontSize = (16 * fontSizeScale).sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(12.dp))

            // 4. The due date (Bottom Subtitle)
            Text(
                text = "$sampleNextMaintText $sampleNextDate",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (14 * fontSizeScale).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun getDaysRemainingColorPreview(): Color {
    return MaterialTheme.colorScheme.primary
}

// You can use this to preview your configuration screen in Android Studio
@Preview(showBackground = true)
@Composable
fun WidgetConfigureScreenPreview() {
    val context = LocalContext.current
    val previewWorkManager = WorkManager.getInstance(context)

    AquaMemoTheme {
        WidgetConfigureScreen(
            appWidgetId = 0,
            workManager = previewWorkManager, // Pass the instance here
            onSave = {}
        )
    }
}