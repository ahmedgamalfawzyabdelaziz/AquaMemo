// CandleDataScreen.kt
package com.ahmedgamal.aquamemo.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.Log
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.ahmedgamal.aquamemo.ads.AdBanner
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandleDataScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToDisplay: () -> Unit,
    filterType: String,
    numberOfCandles: Int,
    candleNumberToEdit: Int = 0
) {

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // منع إعادة التركيب غير الضرورية
    val stableFilterType by remember(filterType) { mutableStateOf(filterType) }
    val stableNumberOfCandles by remember(numberOfCandles) { mutableIntStateOf(numberOfCandles) }
    val stableCandleNumberToEdit by remember(candleNumberToEdit) { derivedStateOf { candleNumberToEdit } }
    val candlesState = remember { mutableStateListOf<Pair<Int, Long>>() }
    var isLoading by remember { mutableStateOf(true) }
    var shouldNavigate by remember { mutableStateOf(false) }
    val successText = context.getString(R.string.data_saved_successfully)
    val errorText = context.getString(R.string.save_error)
    val noDataText = context.getString(R.string.no_data_to_save)
    val gradientColors = remember {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }

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
                    val topBarColors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )

                    TopAppBar(
                        title = {
                            Text(
                                text = if (stableCandleNumberToEdit != 0)
                                    "${stringResource(R.string.edit)} ${getCandleName(stableCandleNumberToEdit)}"
                                else
                                    stringResource(R.string.filter_data_input)
                            )
                        },
                        colors = topBarColors
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                        Text(
                            text = stringResource(R.string.loading_data),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (candlesState.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_data_available),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = candlesState.sortedBy { it.first },
                                key = { it.first }
                            ) { (candleNumber, lastChangedDate) ->
                                CandleDatePickerItem(
                                    candleNumber = candleNumber,
                                    lastChangedDate = lastChangedDate,
                                    onDateSelected = { newDateMillis ->
                                        val index = candlesState.indexOfFirst { it.first == candleNumber }
                                        if (index != -1) {
                                            candlesState[index] = candleNumber to newDateMillis
                                        }
                                    }
                                )
                            }
                        }
                        AdBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                // ✅ احصل على النصوص أولاً خارج الـ coroutine
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (candlesState.isNotEmpty()) {
                                        var success = true
                                        val filtersToSave = mutableListOf<Filter>()

                                        candlesState.forEach { (number, date) ->
                                            try {
                                                val existingFilter = viewModel.getFiltersByType(stableFilterType)
                                                    .first()
                                                    .find { it.candleNumber == number }

                                                val filter = Filter(
                                                    id = existingFilter?.id ?: 0,
                                                    filterType = stableFilterType,
                                                    candleNumber = number,
                                                    lastChangedDate = date
                                                )
                                                filtersToSave.add(filter)
                                            } catch (e: Exception) {
                                                success = false
                                                Log.e("SaveError", "Error saving filter: ${e.message}")
                                            }
                                        }

                                        // حفظ جميع الفلاتر مرة واحدة
                                        if (success && filtersToSave.isNotEmpty()) {
                                            filtersToSave.forEach { filter ->
                                                if (stableCandleNumberToEdit != 0) {
                                                    viewModel.updateFilter(filter)
                                                    // سجل التغيير في التاريخ
                                                    viewModel.addFilterChange(
                                                        FilterChangeHistory(
                                                            filterType = stableFilterType,
                                                            candleNumber = stableCandleNumberToEdit,
                                                            changeDate = System.currentTimeMillis(),
                                                            notes = "تم تحديث ${
                                                                getCandleNameForWorker(
                                                                    stableCandleNumberToEdit,
                                                                    context
                                                                )
                                                            }"
                                                        )
                                                    )
                                                } else {
                                                    viewModel.saveFilter(filter)
                                                }
                                            }

                                            withContext(Dispatchers.Main) {
                                                // ✅ استخدم المتغيرات بدلاً من stringResource() مباشرة
                                                Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                                                shouldNavigate = true
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, noDataText, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = candlesState.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(text = stringResource(R.string.save))
                        }
                    }
                }
            }
        }

    LaunchedEffect(key1 = stableCandleNumberToEdit, key2 = stableFilterType) {
        isLoading = true

        withContext(Dispatchers.IO) {
            try {
                val filters = viewModel.getFiltersByType(stableFilterType).first()
                candlesState.clear()

                if (stableCandleNumberToEdit != 0) {
                    val existingFilter = filters.find { it.candleNumber == stableCandleNumberToEdit }
                    if (existingFilter != null) {
                        candlesState.add(existingFilter.candleNumber to existingFilter.lastChangedDate)
                    } else {
                        candlesState.add(stableCandleNumberToEdit to System.currentTimeMillis())
                    }
                } else {
                    if (filters.isNotEmpty()) {
                        filters.sortedBy { it.candleNumber }.forEach { filter ->
                            candlesState.add(filter.candleNumber to filter.lastChangedDate)
                        }
                    } else {
                        for (i in 1..stableNumberOfCandles) {
                            candlesState.add(i to System.currentTimeMillis())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CandleDataScreen", "Error loading filters: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(key1 = shouldNavigate) {
        if (shouldNavigate) {
            delay(500)
            onNavigateToDisplay()
            shouldNavigate = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandleDatePickerItem(
    candleNumber: Int,
    lastChangedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var currentDate by remember { mutableLongStateOf(lastChangedDate) }

    val displayDate = remember(currentDate) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = getCandleName(candleNumber),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = displayDate,
                onValueChange = { },
                label = { Text(stringResource(R.string.last_change_date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.select_date)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            lastChangedDate = currentDate,
            onDateSelected = { newDateMillis ->
                onDateSelected(newDateMillis)
                currentDate = newDateMillis
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    lastChangedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = lastChangedDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
        yearRange = IntRange(2022, 2040)
    )

    // أسماء الأيام بالإنجليزية
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // العنوان
                Text(
                    text = stringResource(R.string.select_date),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // رأس التقويم - أسماء الأيام
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dayNames.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // خط فاصل
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // DatePicker مع تصميم محسن
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary,
                        navigationContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = null,
                    headline = null
                )

                // خط فاصل
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // أزرار التأكيد والإلغاء
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val selectedDate = datePickerState.selectedDateMillis
                            if (selectedDate != null) {
                                val adjustedDate = Calendar.getInstance().apply {
                                    timeInMillis = selectedDate
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                onDateSelected(adjustedDate)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}