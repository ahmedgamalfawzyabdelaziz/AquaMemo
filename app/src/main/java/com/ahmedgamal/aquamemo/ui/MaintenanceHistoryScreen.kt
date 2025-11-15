// MaintenanceHistoryScreen.kt
package com.ahmedgamal.aquamemo.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ahmedgamal.aquamemo.ads.AdBanner
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(
    onBackClick: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var allFilters by remember { mutableStateOf(emptyList<Filter>()) }
    var sortOrder by remember { mutableStateOf(SortOrder.BY_CANDLE_NUMBER) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterChangeHistory by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf("") }
    var selectedCandleNumber by remember { mutableIntStateOf(0) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewModel.allFilters.collect { filters ->
            allFilters = filters
        }
    }

    // إذا كانت شاشة التاريخ المفصل مفتوحة
    if (showFilterChangeHistory) {
        FilterChangeHistoryScreen(
            filterType = selectedFilterType,
            candleNumber = selectedCandleNumber,
            onBackClick = { showFilterChangeHistory = false }
        )
        return
    }

    val gradientColors = listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.primaryContainer,
        Color(0xFF90CAF9)
    )

    // حساب الفلاتر المرتبة مسبقاً
    val sortedAndGroupedFilters = remember(allFilters, sortOrder, context) {
        sortAndGroupFilters(allFilters, sortOrder, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.maintenance_history),
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
                actions = {
                    // زر خيارات الترتيب
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_options),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_candle)) },
                            onClick = {
                                sortOrder = SortOrder.BY_CANDLE_NUMBER
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date)) },
                            onClick = {
                                sortOrder = SortOrder.BY_DATE
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_name)) },
                            onClick = {
                                sortOrder = SortOrder.BY_NAME
                                showSortMenu = false
                            }
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
        bottomBar = {
            AdBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            )
        }
    )
    { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
        ) {
            if (allFilters.isEmpty()) {
                NoHistoryView()
            } else {
                MaintenanceHistoryList(
                    groupedFilters = sortedAndGroupedFilters,
                    onCandleClick = { filterType, candleNumber ->
                        selectedFilterType = filterType
                        selectedCandleNumber = candleNumber
                        showFilterChangeHistory = true
                    },
                    settingsViewModel = settingsViewModel
                )
                if (allFilters.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // زر مسح السجل
                        Button(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete, // تأكد من استيراد Icons.Default.Delete
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.clear_history))
                        }

                        Text(
                            text = stringResource(R.string.clear_history_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                if (showClearConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showClearConfirmation = false },
                        title = {
                            Text(
                                text = stringResource(R.string.clear_history),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.clear_history_confirmation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showClearConfirmation = false
                                    mainViewModel.clearAllChangeHistory()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.clear_history_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(stringResource(R.string.clear_history))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showClearConfirmation = false }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

// enum للترتيب
enum class SortOrder {
    BY_CANDLE_NUMBER, BY_DATE, BY_NAME
}

// دالة مساعدة للترتيب والتجميع - غير composable
private fun sortAndGroupFilters(
    allFilters: List<Filter>,
    sortOrder: SortOrder,
    context: android.content.Context
): Map<String, List<Filter>> {
    return allFilters.groupBy { it.filterType }
        .mapValues { (_, filters) ->
            when (sortOrder) {
                SortOrder.BY_CANDLE_NUMBER -> filters.sortedBy { it.candleNumber }
                SortOrder.BY_DATE -> filters.sortedByDescending { it.lastChangedDate }
                SortOrder.BY_NAME -> filters.sortedBy {
                    getCandleNameForWorker(it.candleNumber, context)
                }
            }
        }
}

@Composable
fun NoHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_maintenance_history),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = stringResource(R.string.no_history_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun MaintenanceHistoryList(
    groupedFilters: Map<String, List<Filter>>,
    onCandleClick: (String, Int) -> Unit,
    settingsViewModel: SettingsViewModel
) {
    LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedFilters.forEach { (filterType, filters) ->
            item(key = "header_$filterType") {
                FilterTypeHeader(filterType = filterType)
            }

            items(filters, key = { it.id }) { filter ->
                MaintenanceHistoryItem(
                    filter = filter,
                    dateFormatter = dateFormatter,
                    onClick = { onCandleClick(filter.filterType, filter.candleNumber) },
                    settingsViewModel = settingsViewModel
                )
            }

            // فاصل بين أنواع الفلاتر
            item(key = "spacer_$filterType") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FilterTypeHeader(filterType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = filterType,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun MaintenanceHistoryItem(
    filter: Filter,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val intervalMonths by settingsViewModel.getIntervalForCandle(filter.candleNumber)
        .collectAsStateWithLifecycle(initialValue = 3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // اسم الشمعة
            Text(
                text = getCandleName(filter.candleNumber),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // تاريخ التغيير
            HistoryInfoRow(
                label = stringResource(R.string.last_change_date),
                value = dateFormatter.format(Date(filter.lastChangedDate))
            )

            // الفترة بين التغييرات
            HistoryInfoRow(
                label = stringResource(R.string.change_interval),
                value = if (intervalMonths <= 0) "N/A" else context.resources.getQuantityString(
                    R.plurals.months_format,
                    intervalMonths,
                    intervalMonths
                )
            )
            // الموعد القادم
            val nextChangeDate = if (intervalMonths <= 0) null else Calendar.getInstance().apply {
                timeInMillis = filter.lastChangedDate
                add(Calendar.MONTH, intervalMonths)
            }.timeInMillis

            HistoryInfoRow(
                label = stringResource(R.string.next_scheduled_date),
                value = nextChangeDate?.let { dateFormatter.format(Date(it)) } ?: "N/A"
            )
            // رسالة اضغط للتفاصيل
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tap_for_details),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun HistoryInfoRow(label: String, value: String) {
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
            fontWeight = FontWeight.SemiBold
        )
    }
}