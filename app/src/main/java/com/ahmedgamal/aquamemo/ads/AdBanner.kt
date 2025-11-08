package com.ahmedgamal.aquamemo.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import com.google.android.gms.ads.MobileAds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.viewmodel.SettingsViewModel

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val adsRemoved by settingsViewModel.billingManager.adsRemoved.collectAsState(initial = false)
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    var adView by remember { mutableStateOf<AdView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // ✅ الخروج المبكر إذا كان المستخدم Pro - الإضافة الجديدة
    if (adsRemoved) {
        return
    }

    // ✅ Ad Unit ID الحقيقي للإنتاج
    val adUnitId = "ca-app-pub-3238844404804336/4858170154"

    // ✅ تهيئة MobileAds مرة واحدة
    LaunchedEffect(Unit) {
        if (!isInPreview) {
            try {
                MobileAds.initialize(context) {
                    Log.d("AdBanner", "AdMob initialized")
                }
            } catch (e: Exception) {
                loadError = context.getString(R.string.ad_initialization_error, e.message ?: "")
            }
        }
    }
    // ✅ تحميل الإعلان
    LaunchedEffect(Unit) {
        if (!isInPreview && adView == null) {
            try {
                isLoading = true
                loadError = null

                adView = AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isLoading = false
                            loadError = context.getString(
                                R.string.ad_load_error,
                                loadAdError.code,
                                loadAdError.message
                            )
                        }
                        override fun onAdLoaded() {
                            isLoading = false
                            loadError = null
                        }
                    }

                    // ✅ بناء طلب الإعلان للإنتاج
                    val adRequest = AdRequest.Builder().build()
                    loadAd(adRequest)
                }
            } catch (e: Exception) {
                isLoading = false
                val errorMsg = context.getString(R.string.ad_unexpected_error, e.message ?: "")
                loadError = errorMsg
            }
        }
    }

    // ✅ تنظيف الموارد
    DisposableEffect(adView) {
        onDispose {
            adView?.destroy()
        }
    }

    // ✅ واجهة المستخدم
    Column(modifier = modifier) {
        when {
            isInPreview -> PreviewAdBanner()
            isLoading -> LoadingAdBanner()
            loadError != null -> ErrorAdBanner(loadError!!)
            adView != null -> {
                AndroidView(
                    factory = { adView!! },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            else -> LoadingAdBanner()
        }
    }
}

@Composable
private fun PreviewAdBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF4CAF50)),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.ad_preview), color = Color.White)
    }
}

@Composable
private fun LoadingAdBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF2196F3)),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.ad_loading), color = Color.White)
    }
}

@Composable
private fun ErrorAdBanner(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFF44336)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.ad_error), color = Color.White)
            Text(error.take(30) + "...", color = Color.White, fontSize = 10.sp)
        }
    }
}