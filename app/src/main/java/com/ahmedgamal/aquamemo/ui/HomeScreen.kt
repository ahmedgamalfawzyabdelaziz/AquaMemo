package com.ahmedgamal.aquamemo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.ads.AdBanner
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDataDisplay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToTdsTracker: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val allFilters by mainViewModel.allFilters.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val technicianPhone by mainViewModel.technicianPhone.collectAsState()

    var maintenanceList by remember { mutableStateOf(emptyList<MaintenanceInfo>()) }

    // حالة لتخزين اسم الفني (يتم جلبه بشكل غير متزامن)
    var technicianName by remember { mutableStateOf<String?>(null) }

    // حساب مواعيد الصيانة
    LaunchedEffect(allFilters) {
        maintenanceList = getAllMaintenanceInfo(allFilters, context, settingsViewModel)
            .sortedBy { it.daysRemaining }
    }

    // محاولة جلب اسم الفني عند تغير رقم الهاتف
    LaunchedEffect(technicianPhone) {
        if (technicianPhone.isNotEmpty()) {
            technicianName = getContactName(context, technicianPhone)
        }
    }

    HomeScreenContent(
        onNavigateToDataDisplay = onNavigateToDataDisplay,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToTdsTracker = onNavigateToTdsTracker,
        maintenanceList = maintenanceList,
        technicianPhone = technicianPhone,
        technicianName = technicianName // تمرير الاسم
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    onNavigateToDataDisplay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToTdsTracker: () -> Unit,
    maintenanceList: List<MaintenanceInfo>,
    technicianPhone: String,
    technicianName: String?
) {
    val gradientColors = remember {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { maintenanceList.size })

    // منطق الظهور: هل يوجد أي فلتر متبقي له 7 أيام أو أقل؟
    val isUrgentMaintenance = remember(maintenanceList) {
        maintenanceList.any { it.daysRemaining <= 7 }
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
                        IconButton(onClick = onNavigateToTdsTracker) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tds_device),
                                contentDescription = "TDS Tracker",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                // القسم العلوي
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
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

                // القسم السفلي
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // التعديل هنا: يظهر فقط إذا كان هناك رقم + الحالة حرجة (7 أيام أو أقل)
                    if (technicianPhone.isNotEmpty() && isUrgentMaintenance) {
                        TechnicianCard(
                            phoneNumber = technicianPhone,
                            technicianName = technicianName, // تمرير الاسم المكتشف (أو null)
                            onCallClick = { phone ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = "tel:$phone".toUri()
                                }
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    AdBanner(
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
}

// --- المكونات (Composables) ---

@Composable
fun TechnicianCard(
    phoneNumber: String,
    technicianName: String?,
    onCallClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // التعديل هنا: تحديد العنوان بناءً على وجود الاسم
                Text(
                    text = technicianName ?: stringResource(R.string.call_filter_technician),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1 // لضمان عدم تكسير التصميم إذا كان الاسم طويلاً
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Button(
                onClick = { onCallClick(phoneNumber) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone_call),
                    contentDescription = "Call",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_call),
                    color = Color.White
                )
            }
        }
    }
}

// دالة مساعدة لجلب اسم جهة الاتصال (تعمل في الخلفية)
suspend fun getContactName(context: Context, phoneNumber: String): String? {
    // التحقق من صلاحية قراءة جهات الاتصال أولاً لتجنب الكراش
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return null // لا توجد صلاحية، سنعرض النص الافتراضي
    }

    return withContext(Dispatchers.IO) {
        var contactName: String? = null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        contactName = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contactName
    }
}

// --- باقي المكونات كما هي (MaintenancePager, PagerIndicator, NearestMaintenanceCard, etc.) ---
// ... (انسخ باقي الدوال من الملف السابق لعدم الإطالة، فهي لم تتغير) ...

@Composable
fun MaintenancePager(
    maintenanceList: List<MaintenanceInfo>,
    pagerState: PagerState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (maintenanceList.size > 1) {
            PagerIndicator(
                pageCount = maintenanceList.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
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
            Text(
                text = maintenanceInfo.candleName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MaintenanceInfoItem(
                label = stringResource(R.string.next_maintenance_date),
                value = maintenanceInfo.nextChangeDate
            )
            MaintenanceInfoItem(
                label = stringResource(R.string.days_remaining),
                value = maintenanceInfo.daysRemaining.toString()
            )
            LinearProgressIndicator(
                progress = { maintenanceInfo.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
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

@Composable
private fun getPriorityText(daysRemaining: Int, context: Context): String {
    return when {
        daysRemaining <= 7 -> stringResource(R.string.priority_urgent)
        daysRemaining <= 30 -> stringResource(R.string.priority_soon)
        else -> stringResource(R.string.priority_upcoming)
    }
}

data class MaintenanceInfo(
    val candleNumber: Int,
    val candleName: String,
    val nextChangeDate: String,
    val daysRemaining: Int,
    val progress: Float,
    val imageResource: Int,
    val filterType: String
)

private suspend fun getAllMaintenanceInfo(
    filters: List<Filter>,
    context: Context,
    settingsViewModel: SettingsViewModel
): List<MaintenanceInfo> {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentCalendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val currentTimeAtMidnight = currentCalendar.timeInMillis
    return filters.map { filter ->
        val intervalMonths = settingsViewModel.getIntervalForCandle(filter.candleNumber).first()
        if (intervalMonths <= 0) {
            return@map MaintenanceInfo(
                candleNumber = filter.candleNumber,
                candleName = getCandleNameForWorker(filter.candleNumber, context),
                nextChangeDate = "N/A",
                daysRemaining = 0,
                progress = 0f,
                imageResource = getCandleImageResourceDirect(filter.candleNumber),
                filterType = filter.filterType
            )
        }
        val nextChangeCalendar = Calendar.getInstance().apply {
            timeInMillis = filter.lastChangedDate
            add(Calendar.MONTH, intervalMonths)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextChangeDate = nextChangeCalendar.timeInMillis
        val diffMillis = nextChangeDate - currentTimeAtMidnight
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
        val totalDays = (intervalMonths * 30).coerceAtLeast(1)
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