// HomeScreen.kt - الملف الكامل بعد التعديل
package com.ahmedgamal.aquamemo.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.ads.AdBanner
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDataDisplay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val allFilters by mainViewModel.allFilters.collectAsState(initial = emptyList())

        HomeScreenContent(
            onNavigateToDataDisplay = onNavigateToDataDisplay,
            onNavigateToSettings = onNavigateToSettings,
            allFilters = allFilters
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    onNavigateToDataDisplay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    allFilters: List<Filter>
) {
    val gradientColors = remember {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }

    val context = LocalContext.current

    // الحصول على جميع معلومات الصيانة مرتبة حسب الأقرب
    val maintenanceList = remember(allFilters) {
        if (allFilters.isNotEmpty()) {
            getAllMaintenanceInfo(allFilters, context).sortedBy { it.daysRemaining }
        } else {
            emptyList()
        }
    }

    // حالة الـ Pager
    val pagerState = rememberPagerState(pageCount = { maintenanceList.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // العنوان الرئيسي
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight(0.8f)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.next_maintenance),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.next_maintenance_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (maintenanceList.isEmpty()) {
                        NoMaintenanceCard()
                    } else {
                        MaintenancePager(
                            maintenanceList = maintenanceList,
                            pagerState = pagerState
                        )
                    }
                }

                // الإعلان
                AdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                // زر الانتقال لبيانات الشمعات
                Button(
                    onClick = onNavigateToDataDisplay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.view_all_candles),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenancePager(
    maintenanceList: List<MaintenanceInfo>,
    pagerState: PagerState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        // المؤشر (النقاط)
        if (maintenanceList.size > 1) {
            PagerIndicator(
                pageCount = maintenanceList.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // الـ Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val maintenanceInfo = maintenanceList[page]
            NearestMaintenanceCard(
                maintenanceInfo = maintenanceInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        // معلومات الصفحة الحالية
        if (maintenanceList.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${maintenanceList.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(2.dp)
                    .background(
                        color = if (page == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
fun NearestMaintenanceCard(
    maintenanceInfo: MaintenanceInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // صورة الشمعة
            val imageResource = maintenanceInfo.imageResource

            if (imageResource != 0) {
                Image(
                    painter = painterResource(id = imageResource),
                    contentDescription = stringResource(R.string.candle_image),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = stringResource(R.string.candle_image),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // اسم المرحلة
            Text(
                text = maintenanceInfo.candleName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // معلومات الموعد
            MaintenanceInfoItem(
                label = stringResource(R.string.next_maintenance_date),
                value = maintenanceInfo.nextChangeDate
            )

            MaintenanceInfoItem(
                label = stringResource(R.string.days_remaining),
                value = maintenanceInfo.daysRemaining.toString()
            )

            // شريط التقدم
            LinearProgressIndicator(
                progress = { maintenanceInfo.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            // مؤشر الأولوية - مع الترجمة
            Text(
                text = getPriorityText(maintenanceInfo.daysRemaining, context),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                        maintenanceInfo.daysRemaining <= 7 -> MaterialTheme.colorScheme.error
                        maintenanceInfo.daysRemaining <= 30 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                     },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MaintenanceInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NoMaintenanceCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_maintenance_scheduled),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.add_filter_data_to_start),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// دالة مساعدة للأولوية - مع الترجمة
@Composable
private fun getPriorityText(daysRemaining: Int, context: Context): String {
    return when {
        daysRemaining <= 7 -> stringResource(R.string.priority_urgent) // كان يستخدم context.getString
        daysRemaining <= 30 -> stringResource(R.string.priority_soon) // كان يستخدم context.getString
        else -> stringResource(R.string.priority_upcoming) // كان يستخدم context.getString
    }
}

// data class لمعلومات الصيانة
data class MaintenanceInfo(
    val candleNumber: Int,
    val candleName: String,
    val nextChangeDate: String,
    val daysRemaining: Int,
    val progress: Float,
    val imageResource: Int,
    val filterType: String
)

// دالة للحصول على جميع معلومات الصيانة
private fun getAllMaintenanceInfo(
    filters: List<Filter>,
    context: Context
): List<MaintenanceInfo> {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentTime = System.currentTimeMillis()

    return filters.map { filter ->
        val intervalMonths = getCandleIntervalForWorker(filter.candleNumber)
        val nextChangeDate = Calendar.getInstance().apply {
            timeInMillis = filter.lastChangedDate
            add(Calendar.MONTH, intervalMonths)
        }.timeInMillis

        val daysRemaining = ((nextChangeDate - currentTime) / (1000 * 60 * 60 * 24)).toInt()

        val totalDays = intervalMonths * 30
        val progress = (daysRemaining.toFloat() / totalDays).coerceIn(0f, 1f)

        val candleName = getCandleNameForWorker(filter.candleNumber, context)
        val imageResource = getCandleImageResourceDirect(filter.candleNumber)

        MaintenanceInfo(
            candleNumber = filter.candleNumber,
            candleName = candleName,
            nextChangeDate = dateFormatter.format(Date(nextChangeDate)),
            daysRemaining = daysRemaining,
            progress = 1f - progress,
            imageResource = imageResource,
            filterType = filter.filterType
        )
    }
}

// دالة للحصول على الصور مباشرة
private fun getCandleImageResourceDirect(candleNumber: Int): Int {
    return when (candleNumber) {
        1 -> R.drawable.candle_1
        2 -> R.drawable.candle_2
        3 -> R.drawable.candle_3
        4 -> R.drawable.candle_4
        5 -> R.drawable.candle_5
        6 -> R.drawable.candle_6
        7 -> R.drawable.candle_7
        else -> 0
    }
}