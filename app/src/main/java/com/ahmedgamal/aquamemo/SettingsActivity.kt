package com.ahmedgamal.aquamemo

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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

    // Keys for SharedPreferences
    private val REMINDER_HOUR_KEY = "reminder_hour"
    private val REMINDER_MINUTE_KEY = "reminder_minute"
    private val NOTIFICATION_TONE_URI_KEY = "notification_tone_uri"
    private val APP_LANGUAGE_KEY = "selected_language" // **تغيير اسم المفتاح ليتوافق مع باقي الملفات**
    private val NOTIFICATION_PERMISSION_REQUESTED_KEY = "notification_permission_requested" // Ensure this key is defined for filtering in backup

    // Request code for result (deprecated for ActivityResultLauncher but good to know)
    private val REQUEST_CODE_PICK_RINGTONE = 1001 // يمكن استبدالها بـ ActivityResultLauncher

    // SAF: Launcher for creating backup file (Backup)
    private val createBackupFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json") // Specify MIME type
    ) { uri: Uri? ->
        if (uri != null) {
            performBackup(uri)
        } else {
            Toast.makeText(this, getString(R.string.backup_failed_no_location), Toast.LENGTH_SHORT).show()
        }
    }

    // SAF: Launcher for picking backup file (Restore)
    private val pickBackupFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument() // For picking existing document
    ) { uri: Uri? ->
        if (uri != null) {
            confirmRestoreData(uri)
        } else {
            Toast.makeText(this, getString(R.string.select_file_error), Toast.LENGTH_SHORT).show()
        }
    }

    // --- كود تطبيق اللغة: يتم تجاوز attachBaseContext ---
    override fun attachBaseContext(newBase: Context) {
        // استخدم PreferenceManager.getDefaultSharedPreferences للحصول على الشيرد بريفيرنسز
        // هذا يضمن أننا نستخدم نفس مجموعة البيانات التي تخزن اللغة
        val appSharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = appSharedPreferences.getString(APP_LANGUAGE_KEY, "ar") ?: "ar" // الافتراضي عربي

        val config = Configuration(newBase.resources.configuration)
        val locale = Locale(languageCode)
        Locale.setDefault(locale) // لتأثيرات عامة على JVM/أجزاء أخرى
        config.setLocale(locale)

        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        super.attachBaseContext(context)
    }
    // --- نهاية كود تطبيق اللغة ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // لا داعي لاستدعاء applyLanguage هنا، attachBaseContext يتعامل مع ذلك
        setContentView(R.layout.activity_settings)

        // استخدم PreferenceManager.getDefaultSharedPreferences(this) هنا أيضًا
        // لضمان استخدام نفس الـ SharedPreferences الخاصة باللغة والتحكمات الأخرى
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
        val btnAboutApp: Button = findViewById(R.id.btn_about_app) // ربط الزر الجديد
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

// لا تنسَ إضافة هذا السطر لـ text_view_privacy_policy لجعله قابلاً للنقر إذا لم يكن كذلك بالفعل
        val textViewPrivacyPolicy: TextView = findViewById(R.id.text_view_privacy_policy)
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

        // 1. وقت التذكير اليومي
        updateReminderTimeDisplay()
        btnSetReminderTime.setOnClickListener {
            showTimePickerDialog()
        }

        // 2. إدارة أذونات الإشعارات (سابقًا كانت في الرئيسية، يمكن وضع زر هنا فقط)
        btnManageNotificationPermissions.setOnClickListener {
            openNotificationSettings()
        }

        // 3. إعادة جدولة التذكيرات (مفيد بعد تغيير الوقت أو أي بيانات)
        btnRescheduleReminders.setOnClickListener {
            // تأكد من أن ReminderScheduler موجود لديك ويعمل بشكل صحيح
            ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
            Toast.makeText(this, getString(R.string.reminders_rescheduled_success), Toast.LENGTH_SHORT).show()
        }

        // 4. تغيير نغمة الإشعار
        updateNotificationToneDisplay()
        btnChangeNotificationTone.setOnClickListener {
            pickRingtone()
        }

        // 5. اللغة
        setupLanguageSpinner()

        // 6. نسخ احتياطي واستعادة البيانات
        btnBackupData.setOnClickListener {
            // Locale.getDefault() بدلاً من Locale.US لاسم الملف
            createBackupFileLauncher.launch("aquamemo_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)}.json")
        }
        btnRestoreData.setOnClickListener {
            pickBackupFileLauncher.launch(arrayOf("application/json", "text/plain")) // Allow picking JSON or plain text files
        }

        // 7. حول التطبيق
        displayAppVersion()
        textViewPrivacyPolicy.setOnClickListener {
            val url = getString(R.string.privacy_policy_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.read_file_error), Toast.LENGTH_SHORT).show() // يمكن تغيير رسالة الخطأ هنا
            }
        }

        // 8. إعادة تعيين جميع البيانات
        btnResetAllData.setOnClickListener {
            confirmResetAllData()
        }
    }

    private fun showTimePickerDialog() {
        val hour = sharedPreferences.getInt(REMINDER_HOUR_KEY, 9) // Default 9 AM
        val minute = sharedPreferences.getInt(REMINDER_MINUTE_KEY, 0) // Default 0 minutes

        val timePickerDialog = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                sharedPreferences.edit()
                    .putInt(REMINDER_HOUR_KEY, selectedHour)
                    .putInt(REMINDER_MINUTE_KEY, selectedMinute)
                    .apply()
                updateReminderTimeDisplay()
                // تأكد من أن ReminderScheduler موجود لديك ويعمل بشكل صحيح
                ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
                Toast.makeText(this, getString(R.string.reminder_time_saved), Toast.LENGTH_SHORT).show()
            }, hour, minute, false) // false for 12-hour format, true for 24-hour
        timePickerDialog.show()
    }

    private fun updateReminderTimeDisplay() {
        val hour = sharedPreferences.getInt(REMINDER_HOUR_KEY, 9)
        val minute = sharedPreferences.getInt(REMINDER_MINUTE_KEY, 0)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        // Use Locale.getDefault() for user's preferred locale format
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        textViewReminderTime.text = sdf.format(calendar.time)
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> { // Android 8.0 (API 26) and above
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> { // For older versions (less common now)
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
        @Suppress("DEPRECATION") // For startActivityForResult which is deprecated but still used with RingtoneManager
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
                editor.remove(NOTIFICATION_TONE_URI_KEY) // Use default if nothing picked
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
            // التأكد من أن ringtone ليس null قبل استدعاء getTitle
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
        // Fetch language names dynamically based on selected locale
        val languages = listOf(
            getString(R.string.language_arabic),
            getString(R.string.language_english)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        val currentLangCode = sharedPreferences.getString(APP_LANGUAGE_KEY, "ar")
        val selection = if (currentLangCode == "ar") 0 else 1
        spinnerLanguage.setSelection(selection, false) // false to prevent callback on initial setup

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newLangCode = if (position == 0) "ar" else "en"
                val currentLang = sharedPreferences.getString(APP_LANGUAGE_KEY, "ar")
                if (newLangCode != currentLang) {
                    sharedPreferences.edit().putString(APP_LANGUAGE_KEY, newLangCode).apply()
                    // لا داعي لاستدعاء applyLanguage هنا،
                    // recreate() سيعيد بناء الـ Activity و attachBaseContext سيتولى الأمر
                    Toast.makeText(this@SettingsActivity, getString(R.string.language_changed_restart_prompt), Toast.LENGTH_LONG).show()
                    // Recreate the activity to apply language changes
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

    // --- Backup & Restore Logic using SAF ---

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
            // Filter out internal keys not needed for backup
            if (key != NOTIFICATION_PERMISSION_REQUESTED_KEY && key != APP_LANGUAGE_KEY) {
                jsonMap[key] = value
            }
        }
        // Basic JSON string conversion. For robust solutions, consider a JSON library like Gson.
        return jsonMap.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { (key, value) ->
            "\"$key\":" + when (value) {
                is String -> "\"${value.replace("\"", "\\\"")}\"" // Escaping quotes in string values
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
                    // Restart app to fully apply changes
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
        editor.clear() // Clear existing data

        // Basic JSON parsing. For robust JSON, use a library like Gson.
        val entries = jsonString.trim().removePrefix("{").removeSuffix("}").split(",(?=\".*?\":)".toRegex()) // Split by comma not inside quotes
        for (entry in entries) {
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                var valueString = parts[1].trim()

                when {
                    valueString.startsWith("\"") && valueString.endsWith("\"") -> { // String
                        // Unescape quotes in string values
                        editor.putString(key, valueString.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\""))
                    }
                    valueString == "true" -> { // Boolean true
                        editor.putBoolean(key, true)
                    }
                    valueString == "false" -> { // Boolean false
                        editor.putBoolean(key, false)
                    }
                    valueString.matches("-?\\d+\\.\\d+".toRegex()) -> { // Float (or Double)
                        try {
                            editor.putFloat(key, valueString.toFloat())
                        } catch (e: NumberFormatException) {
                            e.printStackTrace() // Log if conversion fails
                        }
                    }
                    valueString.matches("-?\\d+".toRegex()) -> { // Int or Long
                        try {
                            editor.putInt(key, valueString.toInt())
                        } catch (e: NumberFormatException) {
                            try {
                                editor.putLong(key, valueString.toLong())
                            } catch (e2: NumberFormatException) {
                                e2.printStackTrace() // Log if conversion fails
                            }
                        }
                    }
                    // Add more cases for other types or handle null
                    else -> editor.putString(key, valueString) // Fallback for unparsed types
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
                sharedPreferences.edit().clear().apply() // Clear all SharedPreferences
                // تأكد من أن ReminderScheduler موجود لديك ويعمل بشكل صحيح
                ReminderScheduler.scheduleCandleReminders(this, sharedPreferences) // Reschedule/cancel existing reminders
                Toast.makeText(this, getString(R.string.data_reset_success), Toast.LENGTH_LONG).show()
                // Restart app to reflect cleared data in MainActivity
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

    // --- End Backup & Restore Logic ---
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed() // ببساطة العودة إلى الشاشة السابقة
    }

}