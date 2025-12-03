package com.ahmedgamal.aquamemo.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ahmedgamal.aquamemo.AquaMemoApp
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.billing.BillingState
import com.ahmedgamal.aquamemo.utils.LanguageManager
import com.ahmedgamal.aquamemo.viewmodel.MainViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMainScreen: () -> Unit,
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle(initialValue = true)
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle(initialValue = "09:00")
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle(initialValue = "medium")
    val billingState by viewModel.billingManager.billingState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val restoreCompleted by mainViewModel.restoreCompleted.collectAsState()
    val technicianPhone by viewModel.technicianPhone.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // الحالة المحلية لمنع التحديث المستمر أثناء الكتابة
    var phoneInput by remember { mutableStateOf("") }
    rememberScrollState()

    // Collect Pro status
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    LaunchedEffect(restoreCompleted) {
        if (restoreCompleted) {
            LanguageManager.restartApp(activity!!)
        }
    }

    // تحديث الحقل فقط عند فتح الشاشة لأول مرة إذا كانت البيانات موجودة
    LaunchedEffect(technicianPhone) {
        if (phoneInput.isEmpty() && technicianPhone.isNotEmpty()) {
            phoneInput = technicianPhone
        }
    }

    // launcher لاختيار جهة اتصال (Picker) مع استخراج الرقم
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                // الآن الـ URI يشير لجدول الأرقام مباشرة، لذا العمود NUMBER موجود وصحيح
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                try {
                    val cursor = context.contentResolver.query(contactUri, projection, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numberIndex >= 0) {
                                var number = it.getString(numberIndex)
                                // تنظيف الرقم
                                number = number.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

                                phoneInput = number
                                viewModel.saveTechnicianPhone(number)
                                Toast.makeText(context, context.getString(R.string.data_saved_successfully), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Error getting contact number", e)
                }
            }
        }
    }

    // طلب إذن جهات الاتصال
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // إذا تم منح الإذن، نفتح قائمة اختيار الأرقام
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        } else {
            Toast.makeText(context, "يجب منح صلاحية جهات الاتصال لجلب الرقم", Toast.LENGTH_SHORT).show()
        }
    }

    var showMaintenanceHistory by remember { mutableStateOf(false) }
    if (showMaintenanceHistory) {
        MaintenanceHistoryScreen(
            onBackClick = { showMaintenanceHistory = false }
        )
        return
    }
    var showAdvancedStatistics by remember { mutableStateOf(false) }
    if (showAdvancedStatistics) {
        AdvancedStatisticsScreen(
            onBackClick = { showAdvancedStatistics = false }
        )
        return
    }

    val currentLanguageCode = remember {
        mutableStateOf(LanguageManager.getCurrentLanguage(context))
    }

    LaunchedEffect(billingState) {
        when (billingState) {
            BillingState.SUCCESS -> Toast.makeText(context, context.getString(R.string.purchase_successful), Toast.LENGTH_SHORT).show()
            BillingState.ERROR -> Toast.makeText(context, context.getString(R.string.purchase_failed), Toast.LENGTH_SHORT).show()
            BillingState.USER_CANCELED -> Toast.makeText(context, context.getString(R.string.billing_user_canceled), Toast.LENGTH_SHORT).show()
            BillingState.ITEM_ALREADY_OWNED -> Toast.makeText(context, context.getString(R.string.billing_item_owned), Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val gradientColors = remember {
        listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFF90CAF9)
        )
    }

    val arabicText = stringResource(R.string.arabic)
    val englishText = stringResource(R.string.english)
    val smallText = stringResource(R.string.small)
    val mediumText = stringResource(R.string.medium)
    val largeText = stringResource(R.string.large)

    var selectedFontSize by remember {
        mutableStateOf(
            when (fontSize) {
                "small" -> smallText
                "large" -> largeText
                else -> mediumText
            }
        )
    }

    var showFontSizeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(fontSize) {
        selectedFontSize = when (fontSize) {
            "small" -> smallText
            "large" -> largeText
            else -> mediumText
        }
    }

    val backupSuccessMessage = stringResource(R.string.backup_created)
    val backupFailedMessage = stringResource(R.string.backup_failed)
    val restoreSuccessMessage = stringResource(R.string.restore_success)
    val restoreFailedMessage = stringResource(R.string.restore_failed)
    val restoreFailedOpenMessage = stringResource(R.string.restore_failed_open)

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val backupData = mainViewModel.getBackupData()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(backupData.toByteArray())
                    }
                    Toast.makeText(context, backupSuccessMessage, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "$backupFailedMessage: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        mainViewModel.restoreDataFromStream(inputStream)
                        Toast.makeText(context, restoreSuccessMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, restoreFailedOpenMessage, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "$restoreFailedMessage: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                coroutineScope.launch {
                    val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                    sharedPref.edit { putBoolean("has_data", false) }

                    withContext(Dispatchers.IO) {
                        mainViewModel.deleteAllAndResetState()
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.all_data_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    onNavigateToMainScreen()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // حفظ الرقم عند الخروج من الشاشة للتأكيد
    DisposableEffect(phoneInput) {
        onDispose {
            if (phoneInput.isNotEmpty() && phoneInput != technicianPhone) {
                viewModel.saveTechnicianPhone(phoneInput)
            }
        }
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
                            text = stringResource(R.string.settings),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // قسم التذكيرات
                SettingsCard(
                    title = stringResource(R.string.filter_reminders),
                    content = {
                        // صف تفعيل التذكيرات
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null,
                                modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(R.string.enable_reminders), modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary)
                            Switch(
                                checked = remindersEnabled,
                                onCheckedChange = { isEnabled ->
                                    coroutineScope.launch {
                                        viewModel.setRemindersEnabled(isEnabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // زر وقت التذكير
                        OutlinedButton(
                            onClick = { showTimePickerDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${stringResource(R.string.reminder_time)}: $reminderTime")
                        }

                        // إعداد نغمة الإشعار
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = isPro,
                                    onClick = {
                                        try {
                                            createProChannelIfNeeded(context)
                                            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                putExtra(Settings.EXTRA_CHANNEL_ID, AquaMemoApp.PRO_CHANNEL_ID)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, context.getString(R.string.settings_open_fail), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.settings_notification_tone),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = stringResource(R.string.settings_tone_customize),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = if (isPro) 1.0f else 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!isPro) {
                            Text(
                                text = stringResource(R.string.settings_notification_tone_pro_channel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // زر تخصيص مدد الشمعات
                        Button(
                            onClick = {
                                navController.navigate("custom_intervals_screen") {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.custom_candle_intervals))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // قسم المظهر والصوت
                SettingsCard(
                    title = stringResource(R.string.App_appearance),
                    content = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null,
                                modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(R.string.language), modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary)
                            Box {
                                Text(
                                    text = if (currentLanguageCode.value == "ar") arabicText else englishText,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable { showLanguageMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = arabicText, color = MaterialTheme.colorScheme.secondary) },
                                        onClick = {
                                            showLanguageMenu = false
                                            if (activity != null && currentLanguageCode.value != "ar") {
                                                LanguageManager.setLanguage(context, "ar")
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    LanguageManager.restartApp(activity)
                                                }, 300)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = englishText, color = MaterialTheme.colorScheme.secondary) },
                                        onClick = {
                                            showLanguageMenu = false
                                            if (activity != null && currentLanguageCode.value != "en") {
                                                LanguageManager.setLanguage(context, "en")
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    LanguageManager.restartApp(activity)
                                                }, 300)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // حجم الخط
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null,
                                modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(R.string.font_size),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary)
                            Box {
                                Text(
                                    text = selectedFontSize,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable { showFontSizeMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showFontSizeMenu,
                                    onDismissRequest = { showFontSizeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = smallText, color = MaterialTheme.colorScheme.secondary) },
                                        onClick = { viewModel.setFontSize("small"); showFontSizeMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = mediumText, color = MaterialTheme.colorScheme.secondary) },
                                        onClick = { viewModel.setFontSize("medium"); showFontSizeMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = largeText, color = MaterialTheme.colorScheme.secondary) },
                                        onClick = { viewModel.setFontSize("large"); showFontSizeMenu = false }
                                    )
                                }
                            }
                        }

                        var showThemeMenu by remember { mutableStateOf(false) }
                        val currentThemePref by viewModel.themePreference.collectAsStateWithLifecycle()

                        val themeOptions = mapOf(
                            "light" to stringResource(R.string.theme_light),
                            "dark" to stringResource(R.string.theme_dark),
                            "system" to stringResource(R.string.theme_system)
                        )
                        val currentThemeDisplay = themeOptions[currentThemePref] ?: themeOptions["system"]!!

                        Spacer(modifier = Modifier.height(16.dp))

                        // الثيم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = stringResource(R.string.theme),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.theme),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Box {
                                Text(
                                    text = currentThemeDisplay,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable { showThemeMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showThemeMenu,
                                    onDismissRequest = { showThemeMenu = false }
                                ) {
                                    themeOptions.forEach { (key, displayText) ->
                                        DropdownMenuItem(
                                            text = { Text(displayText) },
                                            onClick = {
                                                viewModel.setThemePreference(key)
                                                showThemeMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // كارت الأسعار
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.candle_prices_settings),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        navController.navigate("candle_prices_screen") {
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.update_prices))
                                }

                                Text(
                                    text = stringResource(R.string.customize_prices_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // النسخ الاحتياطي
                SettingsCard(
                    title = stringResource(R.string.backup_restore),
                    content = {
                        Button(
                            onClick = {
                                val fileName = "aquamemo_backup_${System.currentTimeMillis()}.json"
                                backupLauncher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = stringResource(R.string.create_backup))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = stringResource(R.string.restore_from_file))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // سجل الصيانة
                SettingsCard(
                    title = stringResource(R.string.maintenance_history),
                    content = {
                        Button(
                            onClick = { showMaintenanceHistory = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.view_maintenance_history))
                        }

                        Text(
                            text = stringResource(R.string.view_history_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- كارت فني الفلاتر
                SettingsCard(
                    title = stringResource(R.string.technician_contact_title),
                    content = {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                // السماح فقط بالأرقام وعلامة +
                                if (it.all { char -> char.isDigit() || char == '+' || char == ' ' }) {
                                    phoneInput = it
                                }
                            },
                            label = { Text(stringResource(R.string.technician_phone_label)) },
                            placeholder = { Text("010xxxxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.saveTechnicianPhone(phoneInput)
                                    keyboardController?.hide()
                                    Toast.makeText(context, context.getString(R.string.data_saved_successfully), Toast.LENGTH_SHORT).show()
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),

                            // زر اختيار جهة الاتصال داخل الحقل (Trailing Icon)
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_CONTACTS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    } else {
                                        // نستخدم Intent مخصص لاختيار "رقم هاتف" تحديداً
                                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        pickContactLauncher.launch(intent)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "Pick Contact",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            // ✅ ضبط الألوان لتتناسب مع الخلفية الشفافة لـ SettingsCard
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.technician_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // المزايا المميزة (Premium)
                val isPremium by viewModel.billingManager.isPremium.collectAsStateWithLifecycle()
                val productDetails by viewModel.billingManager.subscriptionDetails.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    viewModel.billingManager.loadSubscriptionDetails()
                }

                SettingsCard(
                    title = stringResource(R.string.premium_features),
                    content = {
                        if (isPremium) {
                            Text(
                                text = stringResource(R.string.already_premium),
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            if (productDetails == null) {
                                CircularProgressIndicator()
                            } else {
                                productDetails?.subscriptionOfferDetails?.forEach { offer ->
                                    val pricingPhase = offer.pricingPhases.pricingPhaseList.first()
                                    val price = pricingPhase.formattedPrice
                                    val billingPeriod = pricingPhase.billingPeriod

                                    val label = when (billingPeriod) {
                                        "P1M" -> stringResource(R.string.monthly_plan)
                                        "P1Y" -> stringResource(R.string.yearly_plan)
                                        else -> stringResource(R.string.premium_version)
                                    }

                                    PremiumFeatureItem(
                                        title = label,
                                        description = stringResource(R.string.premium_description),
                                        price = price,
                                        isPurchased = false,
                                        onPurchase = {
                                            viewModel.billingManager.purchaseSubscription(
                                                context as Activity,
                                                offer.offerToken
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.billingManager.restorePurchases()
                                Toast.makeText(context, context.getString(R.string.billing_restoring), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(stringResource(R.string.restore_purchase))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // الإحصائيات المتقدمة
                SettingsCard(
                    title = stringResource(R.string.advanced_statistics),
                    content = {
                        Button(
                            onClick = { showAdvancedStatistics = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.view_statistics))
                        }

                        Text(
                            text = stringResource(R.string.view_statistics_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // زر حذف البيانات
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_all_data),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.delete_all_data))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTimePickerDialog) {
        TimePickerDialog(
            reminderTime = reminderTime,
            onTimeSelected = { time ->
                coroutineScope.launch {
                    viewModel.setReminderTime(time)
                }
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }
}

// ✅ START: Add helper function to create Pro channel from Settings
private fun createProChannelIfNeeded(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // If channel already exists, do nothing.
    if (notificationManager.getNotificationChannel(AquaMemoApp.PRO_CHANNEL_ID) != null) {
        return
    }

    // Create it if it doesn't exist
    Log.d("SettingsScreen", "Pro channel not found. Creating it now.")

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val proName = context.getString(R.string.notification_channel_name_pro)
    val proDescription = context.getString(R.string.notification_channel_description_pro)
    val importance = NotificationManager.IMPORTANCE_HIGH
    val proChannel = NotificationChannel(AquaMemoApp.PRO_CHANNEL_ID, proName, importance).apply {
        description = proDescription
        enableLights(true)
        // ✅ FIX: Use fully qualified name for Android Color
        lightColor = android.graphics.Color.BLUE
        enableVibration(true)
        vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
        setSound(defaultSoundUri, audioAttributes)
    }
    notificationManager.createNotificationChannel(proChannel)
}
@Composable
fun PremiumFeatureItem(
    title: String,
    description: String,
    price: String,
    isPurchased: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchased) Color(0xFFE8F5E8) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPurchased) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPurchased) Color(0xFF2E7D32) else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!isPurchased) {
                Button(
                    onClick = onPurchase,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(stringResource(R.string.subscribe))
                }
            } else {
                Text("✓",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// ... (TimePickerDialog composable remains the same) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    reminderTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timeParts = reminderTime.split(":")
    val initialHour = if (timeParts.size >= 2) timeParts[0].toIntOrNull() ?: 9 else 9
    val initialMinute = if (timeParts.size >= 2) timeParts[1].toIntOrNull() ?: 0 else 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

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
                Text(
                    text = stringResource(R.string.select_reminder_time),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.secondaryContainer,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.surface,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.secondary,
                        selectorColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
                        Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val selectedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                            onTimeSelected(selectedTime)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.confirm), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ... (SettingsCard composable remains the same) ...
@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// ... (DeleteConfirmationDialog composable remains the same) ...
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.delete_all_data)) },
        text = { Text(stringResource(id = R.string.delete_data_confirmation_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(id = R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.no))
            }
        }
    )
}