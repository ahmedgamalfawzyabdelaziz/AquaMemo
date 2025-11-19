// AdvancedStatisticsScreen.kt
package com.ahmedgamal.aquamemo.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import androidx.compose.ui.res.pluralStringResource
import kotlin.collections.sumOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedStatisticsScreen(
    onBackClick: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val isPremium by mainViewModel.billingManager.isPremium.collectAsState()
    var allFilters by remember { mutableStateOf(emptyList<Filter>()) }

    LaunchedEffect(Unit) {
        mainViewModel.allFilters.collect { filters ->
            allFilters = filters
        }
    }

    val gradientColors = listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.primaryContainer,
        Color(0xFF90CAF9)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.advanced_statistics),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color.Transparent,

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
        ) {
            if (!isPremium) {
                PremiumLockedView()
            } else if (allFilters.isEmpty()) {
                NoStatisticsView()
            } else {
                AdvancedStatisticsContent(allFilters = allFilters)
            }
        }
    }
}

@Composable
fun PremiumLockedView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.advanced_statistics),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.premium_feature_locked),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.premium_features_list),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoStatisticsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_statistics_available),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.no_statistics_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AdvancedStatisticsContent(allFilters: List<Filter>) {
    val context = LocalContext.current
    val viewModel: MainViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val candlePrices: List<CandlePrice> by viewModel.candlePrices.collectAsState(emptyList())
    val selectedCurrency: String by viewModel.selectedCurrency.collectAsState()
    var statistics by remember { mutableStateOf<AdvancedStatistics?>(null) }
    LaunchedEffect(allFilters, candlePrices, selectedCurrency) {
        statistics = calculateAdvancedStatistics(
            allFilters,
            context,
            candlePrices,
            selectedCurrency,
            settingsViewModel
        )
    }

    if (statistics == null) {
        // عرض مؤشر تحميل حتى يتم حساب الإحصائيات
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OverviewCard(statistics = statistics!!)
        }

        item {
            ChartsSection(statistics = statistics!!)
        }

        item {
            CostAnalysisCard(statistics = statistics!!)
        }

        item {
            FilterPerformanceCard(statistics = statistics!!)
        }

        item {
            SmartRecommendationsCard(statistics = statistics!!)
        }

        item {
            TrendsAnalysisCard(statistics = statistics!!)
        }
    }
}

@Composable
fun OverviewCard(statistics: AdvancedStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.overview_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    StatisticItem(
                        icon = Icons.Default.WaterDrop,
                        label = stringResource(R.string.total_candles),
                        value = statistics.totalCandles.toString(),
                        color = Color(0xFF2196F3)
                    )
                    StatisticItem(
                        icon = Icons.Default.CalendarMonth,
                        label = stringResource(R.string.average_age),
                        value = pluralStringResource(
                            R.plurals.plurals_days_format,
                            statistics.averageCandleAge,
                            statistics.averageCandleAge
                        ),
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    StatisticItem(
                        icon = Icons.Default.AttachMoney,
                        label = stringResource(R.string.monthly_cost),
                        value = "${statistics.currency} ${statistics.monthlyCost}",
                        color = Color(0xFFFF9800)
                    )
                    StatisticItem(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = stringResource(R.string.usage_efficiency),
                        value = stringResource(R.string.percentage_format, statistics.usageEfficiency),
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartsSection(statistics: AdvancedStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.charts_section),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            val chartHeight = if (statistics.candleDistribution.size <= 1) 120.dp else 220.dp
            // ✅ رسم توزيع الشمعات
            if (statistics.candleDistribution.isNotEmpty()) {
                ImprovedBarChart(
                    title = stringResource(R.string.candle_distribution),
                    data = statistics.candleDistribution,
                    barColor = Color(0xFF2196F3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(vertical = 12.dp)
                )
            } else {
                EmptyChartPlaceholder(stringResource(R.string.candle_distribution))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondaryContainer, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // ✅ رسم توزيع التكلفة
            if (statistics.costDistribution.isNotEmpty()) {
                ImprovedBarChart(
                    title = stringResource(R.string.cost_distribution),
                    data = statistics.costDistribution,
                    barColor = Color(0xFFFF9800),
                    valuePrefix = "${statistics.currency} ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(vertical = 12.dp)
                )
            } else {
                EmptyChartPlaceholder(stringResource(R.string.cost_distribution))
            }
        }
    }
}
// ✅ رسم بياني محسّن مع Animation - الإصدار المصحح
@Composable
fun ImprovedBarChart(
    title: String,
    data: Map<String, Float>,
    barColor: Color,
    valuePrefix: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        if (!animationPlayed) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
            animationPlayed = true
        }
    }

    Column(modifier = modifier) {
        // العنوان مع مؤشر Animation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )

            // مؤشر صغير إن الAnimation شغال
            if (!animationPlayed) {
                Text(
                    text = "⏳",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (data.isEmpty()) {
            EmptyChartPlaceholder(title)
            return
        }

        val maxValue = data.values.maxOrNull() ?: 1f
        val sortedData = data.toList().sortedByDescending { it.second }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            sortedData.forEach { (label, value) ->
                ChartBarItem(
                    label = label,
                    value = value,
                    maxValue = maxValue,
                    barColor = barColor,
                    progress = animatedProgress.value,
                    valuePrefix = valuePrefix,
                    isAnimationComplete = animationPlayed
                )
            }
        }
    }
}

// ✅ عنصر البار مع تحسينات Animation
@Composable
fun ChartBarItem(
    label: String,
    value: Float,
    maxValue: Float,
    barColor: Color,
    progress: Float,
    valuePrefix: String = "",
    isAnimationComplete: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label و Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )

            // القيمة مع effect أثناء الAnimation
            Text(
                text = if (isAnimationComplete) {
                    "$valuePrefix${value.toInt()}"
                } else {
                    "$valuePrefix${(value * progress).toInt()}"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // الخلفية المتدرجة
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / maxValue) * progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.7f),
                                barColor
                            )
                        )
                    )
            ) {
                // النسبة المئوية داخل البار
                if ((value / maxValue) * progress > 0.2f) {
                    Text(
                        text = "${((value / maxValue) * 100 * progress).toInt()}%",
                        color = MaterialTheme.colorScheme.surface,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

// ✅ Placeholder لما مفيش بيانات
@Composable
fun EmptyChartPlaceholder(chartName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_data_available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = chartName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CostAnalysisCard(statistics: AdvancedStatistics) {
    val currencySymbol = when (statistics.currency) {
        "USD" -> "$"
        "EUR" -> "€"
        "EGP" -> "EGP"
        "SAR" -> "﷼"
        else -> "$"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${stringResource(R.string.cost_analysis)} ($currencySymbol)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            StatisticItem(
                icon = Icons.Default.AttachMoney,
                label = stringResource(R.string.total_spent),
                value = "${statistics.currency} ${statistics.totalSpent}",
                color = Color(0xFFF44336)
            )

            StatisticItem(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                label = stringResource(R.string.estimated_savings),
                value = "${statistics.currency} ${statistics.estimatedSavings}",
                color = Color(0xFF4CAF50)
            )

            StatisticItem(
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.yearly_cost),
                value = "${statistics.currency} ${statistics.yearlyCost}",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun FilterPerformanceCard(statistics: AdvancedStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.filter_performance),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            statistics.filterPerformance.forEach { performance ->
                PerformanceRow(
                    filterType = performance.filterType,
                    efficiency = performance.efficiency,
                    averageLifespan = performance.averageLifespan
                )
            }
        }
    }
}

@Composable
fun SmartRecommendationsCard(statistics: AdvancedStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.smart_recommendations),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                )
            }

            statistics.recommendations.forEach { recommendation ->
                RecommendationItem(recommendation = recommendation)
            }
        }
    }
}

@Composable
fun TrendsAnalysisCard(statistics: AdvancedStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.trends_analysis),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            StatisticItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = stringResource(R.string.replacement_rate),
                value = stringResource(R.string.percentage_format, statistics.replacementRate),
                color = Color(0xFF2196F3)
            )

            StatisticItem(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.next_replacement),
                value = statistics.nextReplacement,
                color = Color(0xFFFF9800)
            )

            StatisticItem(
                icon = Icons.Default.Warning,
                label = stringResource(R.string.overdue_candles),
                value = statistics.overdueCandles.toString(),
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun PerformanceRow(
    filterType: String,
    efficiency: Int,
    averageLifespan: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = filterType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val lifespanInMonths = averageLifespan / 30
            Text(
                text = pluralStringResource(
                    R.plurals.plurals_average_lifespan_months,
                    lifespanInMonths,
                    lifespanInMonths
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp * efficiency / 100f)
                    .height(8.dp)
                    .background(
                        color = when {
                            efficiency >= 80 -> Color(0xFF4CAF50)
                            efficiency >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.percentage_format, efficiency),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp)
        )
    }
}

@Composable
fun RecommendationItem(recommendation: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f)
        )
    }
}

data class AdvancedStatistics(
    val totalCandles: Int,
    val totalChanges: Int,
    val averageCandleAge: Int,
    val monthlyCost: Double,
    val usageEfficiency: Int,
    val totalSpent: Double,
    val estimatedSavings: Double,
    val yearlyCost: Double,
    val replacementRate: Int,
    val nextReplacement: String,
    val overdueCandles: Int,
    val candleDistribution: Map<String, Float>,
    val costDistribution: Map<String, Float>,
    val filterPerformance: List<FilterPerformance>,
    val recommendations: List<String>,
    val currency: String = "USD"
)

data class FilterPerformance(
    val filterType: String,
    val efficiency: Int,
    val averageLifespan: Int
)

@SuppressLint("SimpleDateFormat")
private suspend fun calculateAdvancedStatistics(
    filters: List<Filter>,
    context: android.content.Context,
    candlePrices: List<CandlePrice>,
    selectedCurrency: String,
    settingsViewModel: SettingsViewModel
): AdvancedStatistics {
    if (filters.isEmpty()) return AdvancedStatistics(
        totalCandles = 0,
        totalChanges = 0,
        averageCandleAge = 0,
        monthlyCost = 0.0,
        usageEfficiency = 0,
        totalSpent = 0.0,
        estimatedSavings = 0.0,
        yearlyCost = 0.0,
        replacementRate = 0,
        nextReplacement = context.getString(R.string.no_data),
        overdueCandles = 0,
        candleDistribution = emptyMap(),
        costDistribution = emptyMap(),
        filterPerformance = emptyList(),
        recommendations = emptyList(),
        currency = selectedCurrency
    )

    val currentTime = System.currentTimeMillis()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // الإحصائيات الأساسية
    val totalCandles = filters.size
    val totalChanges = filters.sumOf { getChangeCount(it.candleNumber) }

    // حساب متوسط العمر الحالي للشمعات
    val averageCandleAge = filters.map { filter ->
        val ageInDays = (currentTime - filter.lastChangedDate) / (1000 * 60 * 60 * 24)
        ageInDays.toInt()
    }.average().toInt()

    // توزيع الشمعات حسب النوع
    val candleDistribution = filters.groupBy { it.filterType }
        .mapValues { (_, filters) -> filters.size.toFloat() }

    val candlePricesMap = candlePrices.associateBy { it.candleNumber }

    val totalSpent = filters.sumOf { filter ->
        candlePricesMap[filter.candleNumber]?.price ?: 0.0
    }

    val monthlyCost = totalSpent / 12.0
    val yearlyCost = totalSpent

    // كفاءة الاستخدام
    val usageEfficiencySum = filters.count { filter ->
        // 3. التعديل هنا
        val expectedLifespan = settingsViewModel.getIntervalForCandle(filter.candleNumber).first() * 30
        if (expectedLifespan <= 0) return@count true // اعتبرها كفء إذا كانت المدة 0
        val actualAge = (currentTime - filter.lastChangedDate) / (1000 * 60 * 60 * 24)
        actualAge <= expectedLifespan
    }
    val usageEfficiency = (usageEfficiencySum * 100 / totalCandles).coerceIn(0, 100)


    // توزيع التكلفة
    val costDistribution = filters.groupBy { it.filterType }
        .mapValues { (_, filters) ->
            filters.sumOf { candlePricesMap[it.candleNumber]?.price ?: 0.0 }.toFloat()
        }

    // أداء الفلاتر
    val filterPerformance = filters.groupBy { it.filterType }
        .map { (filterType, typeFilters) ->
            val efficiencySum = typeFilters.count { filter ->
                val expectedLifespan = settingsViewModel.getIntervalForCandle(filter.candleNumber).first() * 30
                if (expectedLifespan <= 0) return@count true
                val actualAge = (currentTime - filter.lastChangedDate) / (1000 * 60 * 60 * 24)
                actualAge <= expectedLifespan
            }
            val efficiency = (efficiencySum * 100 / typeFilters.size).coerceIn(0, 100)
            val averageLifespan = typeFilters.map { filter ->
                settingsViewModel.getIntervalForCandle(filter.candleNumber).first() * 30
            }.average().toInt()

            FilterPerformance(filterType, efficiency, averageLifespan)
        }

    // الشمعات المتأخرة
    val overdueCandles = filters.count { filter ->
        val expectedLifespan = settingsViewModel.getIntervalForCandle(filter.candleNumber).first() * 30
        if (expectedLifespan <= 0) return@count false
        val actualAge = (currentTime - filter.lastChangedDate) / (1000 * 60 * 60 * 24)
        actualAge > expectedLifespan
    }

    // التوصيات الذكية
    val recommendations = mutableListOf<String>()

    if (overdueCandles > 0) {
        recommendations.add(
            context.resources.getQuantityString(
                R.plurals.plurals_recommendation_overdue,
                overdueCandles,
                overdueCandles
            )
        )
    }

    if (usageEfficiency < 80) {
        recommendations.add(context.getString(R.string.recommendation_efficiency))
    }

    if (monthlyCost > 100) {
        recommendations.add(context.getString(R.string.recommendation_cost))
    }

    filterPerformance.forEach { performance ->
        if (performance.efficiency < 70) {
            recommendations.add(context.getString(R.string.recommendation_performance, performance.filterType))
        }
    }

    if (recommendations.isEmpty()) {
        recommendations.add(context.getString(R.string.recommendation_excellent))
    }

    // أقرب موعد استبدال
    val validFilters = filters.filter { settingsViewModel.getIntervalForCandle(it.candleNumber).first() > 0 }
    val nextReplacement = validFilters.minByOrNull { filter ->
        val interval = settingsViewModel.getIntervalForCandle(filter.candleNumber).first()
        val nextChangeDate = Calendar.getInstance().apply {
            timeInMillis = filter.lastChangedDate
            add(Calendar.MONTH, interval)
        }.timeInMillis
        nextChangeDate
    }?.let { filter ->
        val interval = settingsViewModel.getIntervalForCandle(filter.candleNumber).first()
        val nextChangeDate = Calendar.getInstance().apply {
            timeInMillis = filter.lastChangedDate
            add(Calendar.MONTH, interval)
        }.timeInMillis
        dateFormatter.format(Date(nextChangeDate))
    } ?: context.getString(R.string.no_data)


    return AdvancedStatistics(
        totalCandles = totalCandles,
        totalChanges = totalChanges,
        averageCandleAge = averageCandleAge,
        monthlyCost = monthlyCost.roundToTwoDecimals(),
        usageEfficiency = usageEfficiency,
        totalSpent = totalSpent.roundToTwoDecimals(),
        estimatedSavings = (totalSpent * 0.15).roundToTwoDecimals(),
        yearlyCost = yearlyCost.roundToTwoDecimals(),
        replacementRate = (totalChanges * 100 / totalCandles).coerceIn(0, 100),
        nextReplacement = nextReplacement,
        overdueCandles = overdueCandles,
        candleDistribution = candleDistribution,
        costDistribution = costDistribution,
        currency = selectedCurrency,
        filterPerformance = filterPerformance,
        recommendations = recommendations,
    )
}

private fun Double.roundToTwoDecimals(): Double {
    val multiplier = 100.0
    return (this * multiplier).roundToInt() / multiplier
}

private fun getChangeCount(candleNumber: Int): Int {
    return when (candleNumber) {
        1 -> 4
        2, 3 -> 2
        4, 6, 7 -> 1
        5 -> 2
        else -> 0
    }
}