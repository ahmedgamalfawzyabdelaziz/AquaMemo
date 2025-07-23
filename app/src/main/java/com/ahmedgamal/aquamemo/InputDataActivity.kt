package com.ahmedgamal.aquamemo

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputDataActivity : AppCompatActivity() {

    private val candleDurations = listOf(
        75,   // الشمعة الأولى
        150,  // الشمعة الثانية
        150,  // الشمعة الثالثة
        630,  // الشمعة الرابعة
        630,  // الشمعة الخامسة
        730,  // الشمعة السادسة
        730   // الشمعة السابعة
    )

    private lateinit var sharedPreferences: SharedPreferences
    private val FILTER_TYPE_KEY = "filter_type"
    private var selectedFilterType: Int = 7
    // المفتاح الخاص باللغة، يجب أن يطابق ما هو في SettingsActivity و MainActivity و SelectFilterActivity
    private val APP_LANGUAGE_KEY = "selected_language"


    private lateinit var btnCandle1Date: Button
    private lateinit var btnCandle2Date: Button
    private lateinit var btnCandle3Date: Button
    private lateinit var btnCandle4Date: Button
    private lateinit var btnCandle5Date: Button
    private lateinit var btnCandle6Date: Button
    private lateinit var btnCandle7Date: Button

    // --- كود تطبيق اللغة: يتم تجاوز attachBaseContext ---
    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = sharedPreferences.getString(APP_LANGUAGE_KEY, "ar") ?: "ar" // الافتراضي عربي

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


    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_data)

        // استخدم PreferenceManager.getDefaultSharedPreferences(this) هنا أيضًا
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)

        btnCandle1Date = findViewById(R.id.btn_select_candle1_date)
        btnCandle2Date = findViewById(R.id.btn_select_candle2_date)
        btnCandle3Date = findViewById(R.id.btn_select_candle3_date)
        btnCandle4Date = findViewById(R.id.btn_select_candle4_date)
        btnCandle5Date = findViewById(R.id.btn_select_candle5_date)
        btnCandle6Date = findViewById(R.id.btn_select_candle6_date)
        btnCandle7Date = findViewById(R.id.btn_select_candle7_date)

        val allDateButtons = listOf(
            btnCandle1Date, btnCandle2Date, btnCandle3Date,
            btnCandle4Date, btnCandle5Date, btnCandle6Date, btnCandle7Date
        )
        val allCandleLabels = listOf(
            findViewById<TextView>(R.id.text_view_candle1_label),
            findViewById<TextView>(R.id.text_view_candle2_label),
            findViewById<TextView>(R.id.text_view_candle3_label),
            findViewById<TextView>(R.id.text_view_candle4_label),
            findViewById<TextView>(R.id.text_view_candle5_label),
            findViewById<TextView>(R.id.text_view_candle6_label),
            findViewById<TextView>(R.id.text_view_candle7_label)
        )

        val layoutCandle4Buttons: View = findViewById(R.id.layout_candle4_buttons)
        val layoutCandle5Buttons: View = findViewById(R.id.layout_candle5_buttons)
        val layoutCandle6Buttons: View = findViewById(R.id.layout_candle6_buttons)
        val layoutCandle7Buttons: View = findViewById(R.id.layout_candle7_buttons)

        val allCandleLayouts = listOf(
            null, null, null,
            layoutCandle4Buttons, layoutCandle5Buttons, layoutCandle6Buttons, layoutCandle7Buttons
        )

        for (i in allDateButtons.indices) {
            val dateButton = allDateButtons[i]
            val labelTextView = allCandleLabels[i]
            val candleLayout = if (i >= 3) allCandleLayouts[i] else null

            if (i >= selectedFilterType) {
                dateButton.visibility = View.GONE
                labelTextView.visibility = View.GONE
                candleLayout?.visibility = View.GONE
            } else {
                dateButton.visibility = View.VISIBLE
                labelTextView.visibility = View.VISIBLE
                candleLayout?.visibility = View.VISIBLE
            }
        }

        val saveButton: Button = findViewById(R.id.btn_save_data)
        val backToMainButton: Button = findViewById(R.id.btn_back_to_main)

        fun showDatePicker(button: Button) {
            val calendar = Calendar.getInstance()
            if (button.text.isNotEmpty()) {
                try {
                    // استخدم Locale.getDefault() بدلاً من Locale.US ليتوافق مع اللغة الحالية
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = dateFormat.parse(button.text.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (button.hint.isNotEmpty()) {
                try {
                    // استخدم Locale.getDefault() بدلاً من Locale.US ليتوافق مع اللغة الحالية
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = dateFormat.parse(button.hint.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    val selectedDateCalendar = Calendar.getInstance()
                    selectedDateCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                    // استخدم Locale.getDefault() بدلاً من Locale.US ليتوافق مع اللغة الحالية
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    button.text = dateFormat.format(selectedDateCalendar.time)
                }, year, month, day)

            datePickerDialog.show()
        }

        allDateButtons.forEach { button ->
            button.setOnClickListener { showDatePicker(button) }
        }

        loadSavedDates(allDateButtons)
        saveButton.setOnClickListener {
            var allDatesEntered = true
            val editor = sharedPreferences.edit()
            for (i in 0 until selectedFilterType) {
                val dateButton = allDateButtons[i]
                val lastChangeDateString = dateButton.text.toString()
                val fixedTimeString = "00:00"

                if (lastChangeDateString.isEmpty()) {
                    allDatesEntered = false
                    break
                }

                try {
                    // استخدم Locale.getDefault() بدلاً من Locale.US ليتوافق مع اللغة الحالية
                    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val lastChangeDateTimeString = "$lastChangeDateString $fixedTimeString"
                    val lastChangeDateTime = dateTimeFormat.parse(lastChangeDateTimeString)
                    if (lastChangeDateTime != null) {
                        editor.putString("candle_${i + 1}_install_date", lastChangeDateString)
                        editor.putString("candle_${i + 1}_install_time", fixedTimeString)
                        val nextChangeCalendar = Calendar.getInstance()
                        nextChangeCalendar.time = lastChangeDateTime
                        nextChangeCalendar.add(Calendar.DAY_OF_YEAR, candleDurations[i])
                        // استخدم Locale.getDefault() بدلاً من Locale.US ليتوافق مع اللغة الحالية
                        editor.putString("candle_${i + 1}_next_change_date", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(nextChangeCalendar.time))
                        editor.putString("candle_${i + 1}_next_change_time", fixedTimeString)

                    } else {
                        allDatesEntered = false
                        Toast.makeText(this, getString(R.string.date_read_error, i + 1), Toast.LENGTH_SHORT).show()
                        break
                    }
                } catch (e: Exception) {
                    allDatesEntered = false
                    Toast.makeText(this, getString(R.string.invalid_date_format, i + 1), Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                    break
                }
            }

            if (allDatesEntered) {
                editor.apply()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
                    } else {
                        Toast.makeText(this, getString(R.string.reminders_might_not_work), Toast.LENGTH_LONG).show()
                    }
                } else {
                    ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(this, getString(R.string.exact_alarm_permission_request), Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    }
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                Toast.makeText(this, getString(R.string.data_saved_and_displayed), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.all_dates_required), Toast.LENGTH_LONG).show()
            }
        }

        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun loadSavedDates(allDateButtons: List<Button>) {
        for (i in 0 until selectedFilterType) {
            val lastChangeDate = sharedPreferences.getString("candle_${i + 1}_install_date", null)
            if (lastChangeDate != null) {
                allDateButtons[i].text = lastChangeDate
            } else {
                allDateButtons[i].text = ""
                allDateButtons[i].hint = getString(R.string.select_date_hint)
            }
        }
    }
}