package com.ahmedgamal.aquamemo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ahmedgamal.aquamemo.MainActivity
import com.ahmedgamal.aquamemo.R

object AquaMemoWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> =
        PreferencesGlanceStateDefinition

    private const val DEFAULT_FONT_SCALE = 1.0f
    private const val DEFAULT_OPACITY = 1.0f
    private val DEFAULT_CORNER_RADIUS = 16.dp

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val themePreference = prefs[WidgetData.themeKey] ?: "system"
            val isPro = (prefs[WidgetData.isProKey] ?: "false").toBoolean()
            val fontScale = prefs[WidgetData.fontSizeScaleKey] ?: DEFAULT_FONT_SCALE
            val backgroundOpacity = prefs[WidgetData.backgroundOpacityKey] ?: DEFAULT_OPACITY
            val cornerRadius = DEFAULT_CORNER_RADIUS


            // جلب النصوص من R.string
            val resources = context.resources
            val nextMaintenanceText = resources.getString(R.string.next_maintenance_widget)
            val noDataTextWidget = resources.getString(R.string.no_data_widget)
            val proFeatureText = resources.getString(R.string.widget_pro_feature)
            val proUnlockText = resources.getString(R.string.widget_pro_unlock)
            val content = @Composable {
                // ✅ --- START OF CHANGE: UNCOMMENT THIS BLOCK ---
                if (isPro) {
                    WidgetContent(
                        context = context,
                        resources = resources,
                        prefs = prefs,
                        nextMaintenanceText = nextMaintenanceText,
                        noDataTextWidget = noDataTextWidget,
                        fontScale = fontScale,
                        backgroundOpacity = backgroundOpacity,
                        cornerRadius = cornerRadius
                    )
                } else {
                    ProUpsellContent(
                        context = context,
                        proFeatureText = proFeatureText,
                        proUnlockText = proUnlockText,
                        fontScale = fontScale,
                        backgroundOpacity = backgroundOpacity,
                        cornerRadius = cornerRadius
                    )
                }
            }

            // Apply the correct theme based on the preference
            when (themePreference) {
                "light" -> GlanceTheme(colors = AppLightGlanceColors) { content() }
                "dark" -> GlanceTheme(colors = AppDarkGlanceColors) { content() }
                else -> GlanceTheme { content() } // 'system' theme
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        resources: android.content.res.Resources,
        prefs: Preferences,
        nextMaintenanceText: String,
        noDataTextWidget: String,
        // ✅ Receive new settings
        fontScale: Float,
        backgroundOpacity: Float,
        cornerRadius: androidx.compose.ui.unit.Dp
    ) {
        val candleName = prefs[WidgetData.candleNameKey]
        val nextDate = prefs[WidgetData.nextDateKey]
        val daysRemaining = prefs[WidgetData.daysRemainingKey]

        // 1. Get the actual Color from the ColorProvider
        val surfaceColor = GlanceTheme.colors.surface.getColor(context)
        // 2. Now we can use .copy() on the Color object
        val backgroundColorWithAlpha = surfaceColor.copy(alpha = backgroundOpacity)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                // ✅ 3. Use the background(Color) overload
                .background(backgroundColorWithAlpha)
                .cornerRadius(cornerRadius) // This rounds the background
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (daysRemaining != null && candleName != null && nextDate != null) {

                // 1. The Filter Name (Top Title)
                Text(
                    text = candleName,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = (18 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(GlanceModifier.height(12.dp))

                // 2. The Big Number (Main Value)
                val daysColor = getDaysRemainingColor(daysRemaining)
                Text(
                    text = daysRemaining.toString(),
                    style = TextStyle(
                        color = daysColor,
                        fontSize = (60 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                // 3. The Label for the number
                Text(
                    text = resources.getQuantityString(R.plurals.days_remaining_widget, daysRemaining, daysRemaining),
                    style = TextStyle(
                        color = daysColor,
                        fontSize = (16 * fontScale).sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(GlanceModifier.height(12.dp))

                // 4. The due date (Bottom Subtitle)
                Text(
                    text = "$nextMaintenanceText $nextDate",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (14 * fontScale).sp,
                        textAlign = TextAlign.Center
                    )
                )

            } else {
                // Fallback if no data is found
                Text(
                    text = noDataTextWidget,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (16 * fontScale).sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @Composable
    private fun ProUpsellContent(
        context: Context,
        proFeatureText: String,
        proUnlockText: String,
        // You can also pass the settings here
        fontScale: Float,
        backgroundOpacity: Float,
        cornerRadius: androidx.compose.ui.unit.Dp
    ) {
        // ✅ Apply same background fix here
        val surfaceColor = GlanceTheme.colors.surface.getColor(context)
        val backgroundColorWithAlpha = surfaceColor.copy(alpha = backgroundOpacity)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColorWithAlpha)
                .cornerRadius(cornerRadius)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒",
                style = TextStyle(fontSize = (24 * fontScale).sp) // Apply scale
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = proFeatureText,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = (16 * fontScale).sp, // Apply scale
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = proUnlockText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = (14 * fontScale).sp, // Apply scale
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun getDaysRemainingColor(days: Int): ColorProvider {
        return when {
            days <= 7 -> GlanceTheme.colors.error // Red for urgent
            days <= 30 -> GlanceTheme.colors.tertiary // Orange for warning
            else -> GlanceTheme.colors.primary // Default color
        }
    }
}