package com.ahmedgamal.aquamemo.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel
import com.google.android.gms.ads.*

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val adsRemoved by settingsViewModel.billingManager.adsRemoved.collectAsState(initial = false)

    // إخفاء الإعلان للمشتركين
    if (adsRemoved) return

    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    var shouldShowAd by remember { mutableStateOf(true) }
    val adUnitId = "ca-app-pub-3238844404804336/4858170154" // Real ID

    if (!shouldShowAd) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (isInPreview) {
            PreviewAdBanner()
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        val adView = AdView(ctx)
                        adView.adUnitId = adUnitId
                        adView.setAdSize(getAdSize(ctx as Activity)) // ✅ الدالة المحدثة

                        adView.adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                shouldShowAd = true
                                Log.d("AdBanner", "Ad loaded successfully")
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                shouldShowAd = false
                                Log.e("AdBanner", "Ad failed: ${error.message}")
                            }
                        }

                        addView(adView)
                        adView.loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
private fun getAdSize(activity: Activity): AdSize {
    // 1. الحصول على كثافة الشاشة (Density)
    val density = activity.resources.displayMetrics.density

    // 2. حساب عرض النافذة بالبكسل حسب إصدار الأندرويد
    val windowWidthPixels = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        // ✅ الطريقة الحديثة (Android 11 / API 30+)
        val windowMetrics = activity.windowManager.currentWindowMetrics
        windowMetrics.bounds.width()
    } else {
        // ⚠️ الطريقة القديمة (للأجهزة القديمة فقط) مع إخفاء التحذير
        @Suppress("DEPRECATION")
        val displayMetrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }

    // 3. تحويل العرض إلى dp
    val adWidth = (windowWidthPixels / density).toInt()

    // 4. إرجاع الحجم المناسب المتجاوب
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
}

@Composable
private fun PreviewAdBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Gray), // ✅ تم التصحيح هنا
        contentAlignment = Alignment.Center
    ) {
        Text("AdMob Adaptive Banner", color = Color.White, textAlign = TextAlign.Center)
    }
}