package com.ahmedgamal.aquamemo.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
@Singleton
class BillingManager
    @Inject
    constructor(
    @param:ApplicationContext private val context: Context
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ✅ حالة الاشتراك
    // 2. Add this new line to force it to "true" for testing
    private val _isPremium = MutableStateFlow(false) // ✅ TODO: change true to false
    // --- END OF CHANGE ---

    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // ✅ حالة الإعلانات
    private val _adsRemoved = MutableStateFlow(false) // ✅ TODO: change true to false
    val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    // ✅ حالة الفوترة
    private val _billingState = MutableStateFlow(BillingState.IDLE)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    // ✅ تفاصيل الاشتراك
    private val _subscriptionDetails = MutableStateFlow<ProductDetails?>(null)
    val subscriptionDetails: StateFlow<ProductDetails?> = _subscriptionDetails.asStateFlow()

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.let { processPurchases(it) }
        } else {
            _billingState.update { BillingState.ERROR }
        }
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.update { BillingState.CONNECTED }
                    checkExistingPurchases()
                    loadSubscriptionDetails()
                } else {
                    _billingState.update { BillingState.ERROR }
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.update { BillingState.DISCONNECTED }
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3000)
                    initializeBillingClient()
                }
            }
        })
    }

    private fun checkExistingPurchases() {
        coroutineScope.launch {
            val subsResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(subsResult.purchasesList)
            }
        }
    }

    fun loadSubscriptionDetails() {
        coroutineScope.launch {
            try {
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("premium_subscription")
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    )
                    .build()

                val result = billingClient.queryProductDetails(params)
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _subscriptionDetails.value = result.productDetailsList?.firstOrNull()
                }
            } catch (e: Exception) {
                Log.e("BillingManager", "Error loading subscription details: ${e.message}")
            }
        }
    }

    fun purchaseSubscription(activity: Activity, offerToken: String) {
        coroutineScope.launch {
            val productDetails = _subscriptionDetails.value ?: return@launch

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        // ✅ Add this line to force premium state even if purchase list is empty
        //_isPremium.update { true } // TODO: REMOVE THIS LINE FOR PRODUCTION
        //_adsRemoved.update { true } // ✅ ALSO FORCE ADS REMOVED HERE FOR TESTING

        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                when {
                    purchase.products.contains("premium_subscription") -> {
                        _isPremium.update { true }
                        _adsRemoved.update { true } // ✅ إخفاء الإعلانات مع الاشتراك
                    }
                    purchase.products.contains("remove_ads") -> {
                        _adsRemoved.update { true }
                    }
                }
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        coroutineScope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }

    fun restorePurchases() {
        checkExistingPurchases()
    }
}

enum class BillingState {
    IDLE, CONNECTED, DISCONNECTED,
    SUCCESS, ERROR,
    USER_CANCELED, ITEM_ALREADY_OWNED
}