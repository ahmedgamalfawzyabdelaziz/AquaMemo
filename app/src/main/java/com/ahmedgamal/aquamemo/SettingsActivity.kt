package com.ahmedgamal.aquamemo

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var textViewReminderTime: TextView
    private lateinit var textViewNotificationTone: TextView
    private lateinit var spinnerLanguage: Spinner
    private lateinit var textViewAppVersion: TextView
    private lateinit var textViewPrivacyPolicy: TextView
    private lateinit var spinnerFontSizeSelection: Spinner

    private val REMINDER_HOUR_KEY = "reminder_hour"
    private val REMINDER_MINUTE_KEY = "reminder_minute"
    private val NOTIFICATION_TONE_URI_KEY = "notification_tone_uri"
    private val NOTIFICATION_PERMISSION_REQUESTED_KEY = "notification_permission_requested"

    private val REQUEST_CODE_PICK_RINGTONE = 1001

    private val createBackupFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            performBackup(uri)
        } else {
            Toast.makeText(this, getString(R.string.backup_failed_no_location), Toast.LENGTH_SHORT).show()
        }
    }

    private val pickBackupFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            confirmRestoreData(uri)
        } else {
            Toast.makeText(this, getString(R.string.select_file_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val appSharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = appSharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar") ?: "ar"
        val fontSizeScale = appSharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f)

        val config = Configuration(newBase.resources.configuration)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)

        config.fontScale = fontSizeScale

        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        super.attachBaseContext(context)
    }

    private lateinit var settingsChangeReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val settingsTitle: TextView = findViewById(R.id.settings_title)
        val btnSetReminderTime: Button = findViewById(R.id.btn_set_reminder_time)
        textViewReminderTime = findViewById(R.id.text_view_reminder_time)
        val btnManageNotificationPermissions: Button = findViewById(R.id.btn_manage_notification_permissions)
        val btnRescheduleReminders: Button = findViewById(R.id.btn_reschedule_reminders)
        val btnChangeNotificationTone: Button = findViewById(R.id.btn_change_notification_tone)
        textViewNotificationTone = findViewById(R.id.text_view_notification_tone)
        spinnerLanguage = findViewById(R.id.spinner_language)
        val btnBackupData: Button = findViewById(R.id.btn_backup_data)
        val btnRestoreData: Button = findViewById(R.id.btn_restore_data)
        textViewAppVersion = findViewById(R.id.text_view_app_version)
        textViewPrivacyPolicy = findViewById(R.id.text_view_privacy_policy)
        val btnResetAllData: Button = findViewById(R.id.btn_reset_all_data)
        val btnAboutApp: Button = findViewById(R.id.btn_about_app)
        spinnerFontSizeSelection = findViewById(R.id.spinner_font_size_selection)

        btnAboutApp.setOnClickListener {
            val versionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "N/A"
            }

            val aboutAppMessage = """
        ${getString(R.string.about_app_description)}

        ${getString(R.string.about_app_version, versionName)}
        ${getString(R.string.about_app_copyright)}
        ${getString(R.string.about_app_acknowledgements)}
    """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_app_title))
                .setMessage(aboutAppMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        textViewPrivacyPolicy.setOnClickListener {
            val privacyPolicyUrl = getString(R.string.privacy_policy_url)
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.read_file_error), Toast.LENGTH_SHORT).show()
            }
        }

        settingsTitle.text = getString(R.string.settings_title)

        updateReminderTimeDisplay()
        btnSetReminderTime.setOnClickListener {
            showTimePickerDialog()
        }

        btnManageNotificationPermissions.setOnClickListener {
            openNotificationSettings()
        }

        btnRescheduleReminders.setOnClickListener {
            ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
            Toast.makeText(this, getString(R.string.reminders_rescheduled_success), Toast.LENGTH_SHORT).show()
        }

        updateNotificationToneDisplay()
        btnChangeNotificationTone.setOnClickListener {
            pickRingtone()
        }

        setupLanguageSpinner()

        val fontSizeDisplayNames = resources.getStringArray(R.array.font_size_options_display_names)
        val fontSizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontSizeDisplayNames)
        fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFontSizeSelection.adapter = fontSizeAdapter

        val savedFontSizeValue = sharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f)
        val fontSizeValues = resources.getStringArray(R.array.font_size_options_values).map { it.toFloat() }
        val savedFontSizeIndex = fontSizeValues.indexOf(savedFontSizeValue)
        if (savedFontSizeIndex != -1) {
            spinnerFontSizeSelection.setSelection(savedFontSizeIndex)
        }

        spinnerFontSizeSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isInitialSelection = true // <--- تعديل هنا: لتجنب الاستدعاء عند التهيئة

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitialSelection) { // <--- تعديل هنا
                    isInitialSelection = false
                    return
                }

                val selectedFontSize = fontSizeValues[position]
                val currentFontSize = sharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f) // <--- تعديل هنا: جلب القيمة الحالية

                if (selectedFontSize != currentFontSize) { // <--- تعديل هنا: قارن لتجنب التغيير بدون داعي
                    val editor = sharedPreferences.edit()
                    editor.putFloat(AppConstants.APP_FONT_SIZE_KEY, selectedFontSize)
                    editor.apply()

                    LocalBroadcastManager.getInstance(this@SettingsActivity)
                        .sendBroadcast(Intent(AppConstants.ACTION_REFRESH_APP_FONT_SIZE))

                    // <--- تعديل هنا: استدعاء recreate() مباشرة بعد إرسال البث
                    // هذا سيجعل SettingsActivity تعيد بناء نفسها لتطبيق حجم الخط
                    recreate()
                    Toast.makeText(this@SettingsActivity, getString(R.string.font_size_changed_restart_prompt), Toast.LENGTH_LONG).show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { /* Do nothing */ }
        }

        btnBackupData.setOnClickListener {
            createBackupFileLauncher.launch("aquamemo_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)}.json")
        }
        btnRestoreData.setOnClickListener {
            pickBackupFileLauncher.launch(arrayOf("application/json", "text/plain"))
        }

        displayAppVersion()

        btnResetAllData.setOnClickListener {
            confirmResetAllData()
        }

        settingsChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // <--- تعديل هنا: حذف recreate() إذا كان الـ Broadcast مصدره SettingsActivity نفسها.
                // الـ recreate() سيتم استدعاؤها في الـ onItemSelectedListener بعد إرسال الـ Broadcast.
                // هذا يمنع الحلقات المفرغة.
                // هذا الـ BroadcastReceiver سيظل يستقبل الإشارات ولكن لن يعيد بناء SettingsActivity
                // إذا كانت هي نفسها التي أطلقت الإشارة.
                // إذا كنت تريد أن SettingsActivity تعيد بناء نفسها *إذا تلقت إشارة من Activity أخرى*
                // فهذه الحالة نادرة في SettingsActivity. يفضل إزالة recreate() هنا لتجنب المشاكل.
                // الـ recreate() في onItemSelectedListener هي الأنسب لتغيير الإعدادات في نفس الـ Activity.
                // الأنشطة الأخرى هي التي يجب أن تستقبل هذه الإشارات وتعيد بناء نفسها.
                when (intent?.action) {
                    AppConstants.ACTION_REFRESH_APP_LANGUAGE -> {
                        // لا تفعل recreate() هنا إذا كانت SettingsActivity هي مصدر التغيير
                        // recreate() تم استدعاؤها بالفعل في setupLanguageSpinner
                    }
                    AppConstants.ACTION_REFRESH_APP_FONT_SIZE -> {
                        // لا تفعل recreate() هنا إذا كانت SettingsActivity هي مصدر التغيير
                        // recreate() تم استدعاؤها بالفعل في spinnerFontSizeSelection.onItemSelectedListener
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(this).apply {
            val intentFilter = IntentFilter()
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_LANGUAGE)
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_FONT_SIZE)
            registerReceiver(settingsChangeReceiver, intentFilter)
        }
    }

    private fun showTimePickerDialog() {
        val hour = sharedPreferences.getInt(REMINDER_HOUR_KEY, 9)
        val minute = sharedPreferences.getInt(REMINDER_MINUTE_KEY, 0)

        val timePickerDialog = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                sharedPreferences.edit()
                    .putInt(REMINDER_HOUR_KEY, selectedHour)
                    .putInt(REMINDER_MINUTE_KEY, selectedMinute)
                    .apply()
                updateReminderTimeDisplay()
                ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
                Toast.makeText(this, getString(R.string.reminder_time_saved), Toast.LENGTH_SHORT).show()
            }, hour, minute, false)
        timePickerDialog.show()
    }

    private fun updateReminderTimeDisplay() {
        val hour = sharedPreferences.getInt(REMINDER_HOUR_KEY, 9)
        val minute = sharedPreferences.getInt(REMINDER_MINUTE_KEY, 0)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        textViewReminderTime.text = sdf.format(calendar.time)
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                }
            }
        }
        startActivity(intent)
    }

    private fun pickRingtone() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.ringtone_picker_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            val existingUriString = sharedPreferences.getString(NOTIFICATION_TONE_URI_KEY, null)
            if (existingUriString != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUriString))
            } else {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_RINGTONE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val editor = sharedPreferences.edit()
            if (uri != null) {
                editor.putString(NOTIFICATION_TONE_URI_KEY, uri.toString())
            } else {
                editor.remove(NOTIFICATION_TONE_URI_KEY)
            }
            editor.apply()
            updateNotificationToneDisplay()
            Toast.makeText(this, getString(R.string.notification_tone_saved), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNotificationToneDisplay() {
        val uriString = sharedPreferences.getString(NOTIFICATION_TONE_URI_KEY, null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            if (ringtone != null) {
                textViewNotificationTone.text = ringtone.getTitle(this)
            } else {
                textViewNotificationTone.text = getString(R.string.default_tone)
            }
        } else {
            textViewNotificationTone.text = getString(R.string.default_tone)
        }
    }

    private fun setupLanguageSpinner() {
        val languages = listOf(
            getString(R.string.language_arabic),
            getString(R.string.language_english)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        val currentLangCode = sharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar")
        val selection = if (currentLangCode == "ar") 0 else 1
        spinnerLanguage.setSelection(selection, false)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isInitialSelection = true // <--- تعديل هنا: لتجنب الاستدعاء عند التهيئة
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitialSelection) { // <--- تعديل هنا
                    isInitialSelection = false
                    return
                }

                val newLangCode = if (position == 0) "ar" else "en"
                val currentLang = sharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar")
                if (newLangCode != currentLang) {
                    sharedPreferences.edit().putString(AppConstants.APP_LANGUAGE_KEY, newLangCode).apply()
                    Toast.makeText(this@SettingsActivity, getString(R.string.language_changed_restart_prompt), Toast.LENGTH_LONG).show()
                    LocalBroadcastManager.getInstance(this@SettingsActivity)
                        .sendBroadcast(Intent(AppConstants.ACTION_REFRESH_APP_LANGUAGE))
                    // <--- تعديل هنا: استدعاء recreate() مباشرة بعد إرسال البث
                    // هذا سيجعل SettingsActivity تعيد بناء نفسها لتطبيق اللغة
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { /* Do nothing */ }
        }
    }

    private fun displayAppVersion() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            textViewAppVersion.text = getString(R.string.app_version_label, version)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            textViewAppVersion.text = getString(R.string.app_version_label, "N/A")
        }
    }

    private fun performBackup(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val json = getSharedPreferencesAsJson()
                    writer.write(json)
                }
            }
            Toast.makeText(this, getString(R.string.backup_success), Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.backup_failed) + ": ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getSharedPreferencesAsJson(): String {
        val allEntries = sharedPreferences.all
        val jsonMap = mutableMapOf<String, Any?>()
        for ((key, value) in allEntries) {
            if (key != NOTIFICATION_PERMISSION_REQUESTED_KEY) {
                jsonMap[key] = value
            }
        }
        return jsonMap.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { (key, value) ->
            "\"$key\":" + when (value) {
                is String -> "\"${value.replace("\"", "\\\"")}\""
                is Int, is Long, is Boolean, is Float -> value.toString()
                else -> "null"
            }
        }
    }

    private fun confirmRestoreData(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restore_confirm_title))
            .setMessage(getString(R.string.restore_confirm_message))
            .setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface, which: Int ->
                performRestore(uri)
            }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performRestore(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonString = reader.readText()
                    applyJsonToSharedPreferences(jsonString)
                    Toast.makeText(this, getString(R.string.restore_success), Toast.LENGTH_LONG).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.read_file_error) + ": ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.restore_failed) + ": ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyJsonToSharedPreferences(jsonString: String) {
        val editor = sharedPreferences.edit()
        editor.clear()

        val entries = jsonString.trim().removePrefix("{").removeSuffix("}").split(",(?=\".*?\":)".toRegex())
        for (entry in entries) {
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                var valueString = parts[1].trim()

                when {
                    valueString.startsWith("\"") && valueString.endsWith("\"") -> {
                        editor.putString(key, valueString.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\""))
                    }
                    valueString == "true" -> {
                        editor.putBoolean(key, true)
                    }
                    valueString == "false" -> {
                        editor.putBoolean(key, false)
                    }
                    valueString.matches("-?\\d+\\.\\d+".toRegex()) -> {
                        try {
                            editor.putFloat(key, valueString.toFloat())
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    valueString.matches("-?\\d+".toRegex()) -> {
                        try {
                            editor.putInt(key, valueString.toInt())
                        } catch (e: NumberFormatException) {
                            try {
                                editor.putLong(key, valueString.toLong())
                            } catch (e2: NumberFormatException) {
                                e2.printStackTrace()
                            }
                        }
                    }
                    else -> editor.putString(key, valueString)
                }
            }
        }
        editor.apply()
    }

    private fun confirmResetAllData() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_confirm_title))
            .setMessage(getString(R.string.reset_confirm_message))
            .setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface, which: Int ->
                sharedPreferences.edit().clear().apply()
                ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
                Toast.makeText(this, getString(R.string.data_reset_success), Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsChangeReceiver)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}