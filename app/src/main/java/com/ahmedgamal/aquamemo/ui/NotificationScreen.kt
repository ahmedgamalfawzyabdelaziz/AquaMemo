package com.ahmedgamal.aquamemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import com.ahmedgamal.aquamemo.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

// ✅ 1. (جديد) هذا هو الـ Import الصحيح
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val color1 = MaterialTheme.colorScheme.secondaryContainer
    val color2 = MaterialTheme.colorScheme.primaryContainer
    val gradientColors = remember(color1, color2) {
        listOf(
            color1,
            color2,
            Color(0xFF90CAF9)
        )
    }

    val notificationsList by viewModel.allNotifications.collectAsStateWithLifecycle()

    // --- (جديد) كود السحب للتحديث (بالطريقة الصحيحة) ---
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    // (لا نحتاج لـ state أو coroutineScope هنا)
    // ---

    // --- (جديد) كود مربع حوار الحذف ---
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    if (showConfirmDialog) {
        DeleteAllConfirmationDialog(
            onConfirm = {
                viewModel.clearAllNotifications()
                showConfirmDialog = false
            },
            onDismiss = {
                showConfirmDialog = false
            }
        )
    }
    // ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
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
                    val hasUnread = notificationsList.any { !it.isRead }
                    if (hasUnread) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                Icons.Default.MarkAsUnread,
                                contentDescription = stringResource(R.string.mark_all_as_read),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (notificationsList.isNotEmpty()) {
                        IconButton(onClick = { showConfirmDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.delete_all_notifications),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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

        // ✅ 2. (معدل) استخدام "PullToRefreshBox" بدلاً من "Box"
        PullToRefreshBox(
            isRefreshing = isRefreshing, // تمرير الحالة
            onRefresh = { viewModel.refresh() }, // تمرير دالة الاستدعاء
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(brush = Brush.verticalGradient(colors = gradientColors))
        ) {

            // (الآن نضع المحتوى بداخل هذا الـ Box)
            if (notificationsList.isEmpty()) {
                // يجب وضع EmptyView داخل Box إضافي لتوسيطه
                Box(modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())) { // إضافة سحب وهمي
                    EmptyNotificationsView()
                }
            } else {
                NotificationHistoryList(
                    notifications = notificationsList
                )
            }

            // (مؤشر التحديث أصبح مدمجاً، لا داعي لإضافته يدوياً)
        }
    }
}

// ✅ 3. (محذوف) قمنا بحذف دالة PullToRefreshContainer الفارغة

@Composable
fun NotificationHistoryList(
    notifications: List<NotificationHistory>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(notifications, key = { it.id }) { notification ->
            NotificationItem(notification = notification)
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationHistory) {

    val icon: ImageVector
    val iconColor: Color
    when (notification.iconType) {
        "CANDLE" -> {
            icon = Icons.Default.WaterDrop
            iconColor = MaterialTheme.colorScheme.primary
        }
        "INFO" -> {
            icon = Icons.Default.Info
            iconColor = MaterialTheme.colorScheme.secondary
        }
        else -> {
            icon = Icons.Default.NotificationsActive
            iconColor = MaterialTheme.colorScheme.tertiary
        }
    }

    val cardAlpha = if (notification.isRead) 0.6f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatTimestamp(notification.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    // (الكود الذي تم إصلاحه سابقاً)
    val sdfDate = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val yesterdayText = stringResource(R.string.yesterday)

    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)

    calendar.timeInMillis = timestamp
    val notificationDay = calendar.get(Calendar.DAY_OF_YEAR)

    return when {
        today == notificationDay -> sdfTime.format(Date(timestamp))
        today - notificationDay == 1 -> yesterdayText
        else -> sdfDate.format(Date(timestamp))
    }
}

@Composable
fun EmptyNotificationsView() {
    // (كما هي)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_notifications_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.no_notifications_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// (جديد) مربع حوار تأكيد الحذف
@Composable
private fun DeleteAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_all_notifications)) },
        text = { Text(stringResource(R.string.delete_all_notifications_confirm)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}