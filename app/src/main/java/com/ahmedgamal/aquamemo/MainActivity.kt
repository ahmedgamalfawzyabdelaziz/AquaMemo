package com.ahmedgamal.aquamemo

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.content.BroadcastReceiver // لإضافة الـ BroadcastReceiver
import android.content.IntentFilter // لإضافة الـ BroadcastReceiver
import androidx.localbroadcastmanager.content.LocalBroadcastManager // لإضافة الـ LocalBroadcastManager
import com.ahmedgamal.aquamemo.SettingsActivity
import com.ahmedgamal.aquamemo.AppConstants

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var layoutExpiringCandles: LinearLayout
    private lateinit var cardNextCandleInfo: CardView
    private lateinit var layoutExpiringCandlesTextDetails: LinearLayout
    private lateinit var settingsChangeReceiver: BroadcastReceiver

    private val candleReplacementPeriods = listOf(3, 6, 6, 21, 21, 24, 24)
    private val FILTER_TYPE_KEY = "filter_type"
    private var selectedFilterType: Int = 7
    private val EXPIRATION_WARNING_DAYS = 4

    // مفتاح لتخزين حالة ما إذا كان إذن الإشعارات قد طُلب بالفعل
    private val NOTIFICATION_PERMISSION_REQUESTED_KEY = "notification_permission_requested"
    // المفتاح الخاص باللغة، يجب أن يطابق ما هو في SettingsActivity
    private val APP_LANGUAGE_KEY = "selected_language"

    // *************************************************************
    // requestPermissionLauncher تم نقله إلى MainActivity
    // *************************************************************
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.permission_granted_success), Toast.LENGTH_SHORT).show()
            // إذا تم منح الإذن، قم بجدولة التذكيرات (لضمان عملها فوراً)
            ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
        } else {
            Toast.makeText(this, getString(R.string.permission_denied_warning), Toast.LENGTH_LONG).show()
        }
    }
    // *************************************************************

    // --- كود تطبيق اللغة: يتم تجاوز attachBaseContext ---
    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        // استخدم الثوابت من SettingsActivity للوصول إلى قيم الشيرد بريفيرنسز
        val languageCode = sharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar") ?: "ar"
        val fontSizeScale = sharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f)
        val config = Configuration(newBase.resources.configuration)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)

        config.fontScale = fontSizeScale // تطبيق حجم الخط

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
        setContentView(R.layout.activity_main)
        // تهيئة وتسجيل الـ BroadcastReceiver للاستماع لتغيرات الإعدادات (اللغة وحجم الخط)
        settingsChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AppConstants.ACTION_REFRESH_APP_LANGUAGE -> { // استخدم AppConstants
                        recreate() // إعادة إنشاء النشاط لتحديث اللغة
                    }
                    AppConstants.ACTION_REFRESH_APP_FONT_SIZE -> { // استخدم AppConstants
                        recreate() // إعادة إنشاء النشاط لتحديث حجم الخط
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(this).apply {
            val intentFilter = IntentFilter()
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_LANGUAGE)
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_FONT_SIZE)
            registerReceiver(settingsChangeReceiver, intentFilter)
        }// تهيئة BroadcastReceiver للاستماع لتغيرات الإعدادات (اللغة وحجم الخط)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)

        layoutExpiringCandles = findViewById(R.id.layout_expiring_candles)
        cardNextCandleInfo = findViewById(R.id.card_next_candle_info)
        layoutExpiringCandlesTextDetails = findViewById(R.id.layout_expiring_candles_text_details)

        val btnChooseFilterType: Button = findViewById(R.id.btn_choose_filter_type)
        val btnModifyData: Button = findViewById(R.id.btn_modify_data)
        val btnShowDashboard: Button = findViewById(R.id.btn_show_dashboard)
        val btnSettings: ImageButton = findViewById(R.id.btn_settings)

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnChooseFilterType.setOnClickListener {
            val intent = Intent(this, SelectFilterActivity::class.java)
            startActivity(intent)
        }

        btnModifyData.setOnClickListener {
            val intent = Intent(this, InputDataActivity::class.java)
            startActivity(intent)
        }

        btnShowDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        // *************************************************************
        // إضافة منطق طلب إذن الإشعارات هنا عند إنشاء النشاط
        // *************************************************************
        requestNotificationPermissionOnce()
        // *************************************************************
    }
    override fun onResume() {
        super.onResume()
        selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)
        updateCandleDisplayForNextChange()
    }
    private fun requestNotificationPermissionOnce() {
        // نتحقق أولاً ما إذا كان إصدار Android هو 13 أو أعلى
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // نتحقق إذا كان الإذن ممنوحاً بالفعل
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // الإذن ممنوح، لا تفعل شيئاً سوى التأكد من جدولة التذكيرات
                ReminderScheduler.scheduleCandleReminders(this, sharedPreferences)
            } else {
                // الإذن ليس ممنوحاً. نتحقق ما إذا كان قد تم طلبه بالفعل لمرة واحدة
                val permissionAlreadyRequested = sharedPreferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED_KEY, false)

                if (!permissionAlreadyRequested) {
                    // لم يتم طلب الإذن بعد في المرة الأولى. قم بطلبه.
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    // سجل أن الإذن قد طُلب الآن لمنعه من الظهور مرة أخرى تلقائياً
                    sharedPreferences.edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED_KEY, true).apply()
                }
                // إذا كان قد تم طلبه مسبقاً ورفضه، فلن يظهر المربع مرة أخرى تلقائياً.
                // يمكن للمستخدم الذهاب إلى الإعدادات لتمكينه.
                // يمكنك هنا إضافة منطق لشرح للمستخدم كيفية تمكينه يدوياً إذا لزم الأمر
            }
        }
    }
    private fun updateCandleDisplayForNextChange() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        val soonExpiringCandles = mutableListOf<Triple<Int, Long, String>>()
        var overallMinDaysLeft = Long.MAX_VALUE
        var nextCandleToDisplay: Int = -1
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0 until selectedFilterType) {
            val installDateString = sharedPreferences.getString("candle_${i + 1}_install_date", null)
            val installTimeString = sharedPreferences.getString("candle_${i + 1}_install_time", "00:00")

            if (installDateString != null) {
                try {
                    val installDateTimeString = "$installDateString $installTimeString"
                    val installDate = dateFormat.parse(installDateTimeString)

                    if (installDate != null) {
                        val nextChangeDateCalendar = Calendar.getInstance().apply {
                            time = installDate
                            add(Calendar.MONTH, candleReplacementPeriods.getOrNull(i) ?: 0)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val diffMillis = nextChangeDateCalendar.timeInMillis - today.timeInMillis
                        var daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)

                        if (daysLeft < 0) {
                            daysLeft = 0L
                        }

                        if (daysLeft <= EXPIRATION_WARNING_DAYS) {
                            soonExpiringCandles.add(Triple(i + 1, daysLeft, installDateString))
                        }

                        if (daysLeft < overallMinDaysLeft) {
                            overallMinDaysLeft = daysLeft
                            nextCandleToDisplay = i + 1
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        layoutExpiringCandles.removeAllViews()
        layoutExpiringCandlesTextDetails.removeAllViews()

        if (soonExpiringCandles.isNotEmpty()) {
            soonExpiringCandles.sortBy { it.second }

            for (candleInfo in soonExpiringCandles) {
                val candleNumber = candleInfo.first
                val imageView = ImageView(this)
                val layoutParams = LinearLayout.LayoutParams(225, 225)
                layoutParams.marginEnd = -40
                layoutParams.marginStart = -40
                imageView.layoutParams = layoutParams
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                val candleImageResId = when (candleNumber) {
                    1 -> R.drawable.candle_1
                    2 -> R.drawable.candle_2
                    3 -> R.drawable.candle_3
                    4 -> R.drawable.candle_4
                    5 -> R.drawable.candle_5
                    6 -> R.drawable.candle_6
                    7 -> R.drawable.candle_7
                    else -> R.drawable.candle_placeholder
                }
                imageView.setImageResource(candleImageResId)
                layoutExpiringCandles.addView(imageView)
            }

            val titleTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8.dpToPx())
                }
                text = getString(R.string.candles_requiring_attention_list_title)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.black, theme))
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutExpiringCandlesTextDetails.addView(titleTextView)

            for (candleInfo in soonExpiringCandles) {
                val candleNumber = candleInfo.first
                val daysLeft = candleInfo.second
                val candleChangeInfoText = if (daysLeft == 0L) {
                    getString(R.string.candle_change_info_today, candleNumber)
                } else {
                    getString(R.string.candle_change_info, candleNumber, daysLeft)
                }

                val detailTextView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4.dpToPx(), 0, 4.dpToPx())
                    }
                    text = candleChangeInfoText
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
                layoutExpiringCandlesTextDetails.addView(detailTextView)
            }

            cardNextCandleInfo.visibility = View.VISIBLE
        } else if (nextCandleToDisplay != -1) {
            val candleImageResId = when (nextCandleToDisplay) {
                1 -> R.drawable.candle_1
                2 -> R.drawable.candle_2
                3 -> R.drawable.candle_3
                4 -> R.drawable.candle_4
                5 -> R.drawable.candle_5
                6 -> R.drawable.candle_6
                7 -> R.drawable.candle_7
                else -> R.drawable.candle_placeholder
            }
            val imageView = ImageView(this)
            val imageLayoutParams = LinearLayout.LayoutParams(220, 220)
            imageView.layoutParams = imageLayoutParams
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.setImageResource(candleImageResId)
            layoutExpiringCandles.addView(imageView)

            val nextCandleLabelTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 4.dpToPx())
                }
                text = getString(R.string.next_candle_label, nextCandleToDisplay)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.black, theme))
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutExpiringCandlesTextDetails.addView(nextCandleLabelTextView)

            val daysLeftTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4.dpToPx(), 0, 0)
                }
                text = if (overallMinDaysLeft == 0L) {
                    getString(R.string.days_left_message_today)
                } else {
                    getString(R.string.days_left_message, overallMinDaysLeft)
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutExpiringCandlesTextDetails.addView(daysLeftTextView)

            cardNextCandleInfo.visibility = View.VISIBLE
        } else {
            layoutExpiringCandles.removeAllViews()
            layoutExpiringCandlesTextDetails.removeAllViews()
            val noDataTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = getString(R.string.no_candle_data_yet)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTextColor(resources.getColor(android.R.color.black, theme))
                gravity = Gravity.CENTER
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }
            layoutExpiringCandlesTextDetails.addView(noDataTextView)

            cardNextCandleInfo.visibility = View.GONE
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density + 0.5f).toInt()
    }
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_app_title))
            .setMessage(getString(R.string.exit_app_message))
            .setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface, which: Int ->
                // استخدام finishAffinity() لإغلاق جميع الأنشطة المرتبطة بالتطبيق والخروج تمامًا.
                finishAffinity()
            }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, which: Int ->
                dialog.dismiss() // إغلاق مربع الحوار دون فعل أي شيء آخر
            }
            .show()
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsChangeReceiver)
    }
}