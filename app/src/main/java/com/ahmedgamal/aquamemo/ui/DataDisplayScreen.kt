// DataDisplayScreen.kt
package com.ahmedgamal.aquamemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.ahmedgamal.aquamemo.R
import androidx.compose.ui.text.font.FontWeight
import com.ahmedgamal.aquamemo.ads.AdBanner


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataDisplayScreen(
    onNavigateToEdit: (String, Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // تعريف التدرج اللوني المتسق
    val gradientColors = remember {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }

    val allFilters by viewModel.allFilters.collectAsStateWithLifecycle(initialValue = emptyList())

    val groupedFilters by remember(allFilters) {
        derivedStateOf { allFilters.groupBy { it.filterType } }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = gradientColors))
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.filter_data),
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
                                    Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    // ✅ الإعلان في الـ BottomBar
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    )
                }
            ) { innerPadding ->
                if (groupedFilters.isEmpty()) {
                    NoDataView(modifier = Modifier.padding(innerPadding))
                } else {
                    FiltersListView(
                        groupedFilters = groupedFilters,
                        dateFormatter = dateFormatter,
                        onNavigateToEdit = onNavigateToEdit,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
}

@Composable
private fun NoDataView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_data_available),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(32.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FiltersListView(
    groupedFilters: Map<String, List<Filter>>,
    dateFormatter: SimpleDateFormat,
    onNavigateToEdit: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        groupedFilters.forEach { (filterType, filters) ->
            item(key = "header_$filterType") {
                Text(
                    text = filterType,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(
                items = filters.sortedBy { it.candleNumber },
                key = { it.id }
            ) { filter ->
                FilterCard(
                    filter = filter,
                    dateFormatter = dateFormatter,
                    onNavigateToEdit = onNavigateToEdit
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    filter: Filter,
    dateFormatter: SimpleDateFormat,
    onNavigateToEdit: (String, Int) -> Unit
) {
    val context = LocalContext.current

    val lastChangedDate by remember(filter.lastChangedDate) {
        derivedStateOf { dateFormatter.format(Date(filter.lastChangedDate)) }
    }

    val nextChangeDate by remember(filter.lastChangedDate, filter.candleNumber) {
        derivedStateOf {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = filter.lastChangedDate
                add(Calendar.MONTH, getCandleIntervalForWorker(filter.candleNumber))
            }
            dateFormatter.format(calendar.time)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getCandleNameForWorker(filter.candleNumber, context), // ✅ التصحيح
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            DateRow(label = stringResource(R.string.last_change), value = lastChangedDate)
            Spacer(modifier = Modifier.height(8.dp))
            DateRow(label = stringResource(R.string.next_change), value = nextChangeDate)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigateToEdit(filter.filterType, filter.candleNumber) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(R.string.edit_data))
            }
        }
    }
}

@Composable
private fun DateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}