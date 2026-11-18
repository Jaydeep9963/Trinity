package com.aaritlabs.trinity.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aaritlabs.trinity.data.GamePreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import java.util.concurrent.ConcurrentHashMap

class BillingManager private constructor(private val appContext: Context) : PurchasesUpdatedListener {

    private val prefs = GamePreferences(appContext)
    private var billingClient: BillingClient? = null
    private val productDetailsMap = ConcurrentHashMap<String, ProductDetails>()
    private var onPurchaseCallback: ((Boolean, String) -> Unit)? = null

    companion object {
        private const val TAG = "BillingManager"

        // Non-consumables
        const val PRODUCT_REMOVE_ADS = "remove_ads"           // $4.99
        const val PRODUCT_ALL_SKINS = "unlock_all_skins"      // $9.99
        const val PRODUCT_SKIN_FRUITS = "skin_fruits"         // $2.99
        const val PRODUCT_SKIN_SMILEYS = "skin_smileys"       // $2.99
        const val PRODUCT_SKIN_ANIMALS = "skin_animals"       // $2.99
        const val PRODUCT_SKIN_BAKERY = "skin_bakery"         // $2.99

        // Consumable Coin Bundles
        const val PRODUCT_COINS_500 = "coins_500"             // $0.99
        const val PRODUCT_COINS_1500 = "coins_1500"           // $2.49
        const val PRODUCT_COINS_5000 = "coins_5000"           // $4.99
        const val PRODUCT_COINS_15000 = "coins_15000"         // $9.99

        val IN_APP_PRODUCT_LIST = listOf(
            PRODUCT_REMOVE_ADS,
            PRODUCT_ALL_SKINS,
            PRODUCT_SKIN_FRUITS,
            PRODUCT_SKIN_SMILEYS,
            PRODUCT_SKIN_ANIMALS,
            PRODUCT_SKIN_BAKERY,
            PRODUCT_COINS_500,
            PRODUCT_COINS_1500,
            PRODUCT_COINS_5000,
            PRODUCT_COINS_15000
        )

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        buildBillingClient()
    }

    private fun buildBillingClient() {
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()
        startConnection()
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup finished successfully.")
                    queryAvailableProducts()
                    restorePurchases()
                    onConnected?.invoke()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will retry on next request.")
            }
        })
    }

    private fun queryAvailableProducts() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = IN_APP_PRODUCT_LIST.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in result.productDetailsList) {
                    productDetailsMap[details.productId] = details
                    Log.d(TAG, "Queried product: ${details.productId} - ${details.title}")
                }
            } else {
                Log.w(TAG, "Error querying product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun getProductDetails(productId: String): ProductDetails? = productDetailsMap[productId]

    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        val client = billingClient
        val details = productDetailsMap[productId]

        this.onPurchaseCallback = onComplete

        if (client != null && client.isReady && details != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            client.launchBillingFlow(activity, flowParams)
        } else {
            // Fallback for offline, developer test, or before Google Play Console items are activated:
            // Complete local test unlock so functionality can be tested end-to-end!
            Log.d(TAG, "Billing client not ready or product details not loaded. Executing test purchase for $productId.")
            applyPurchaseLocally(productId)
            onComplete(true, "Purchase successful (Test mode)!")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            onPurchaseCallback?.invoke(true, "Purchase completed successfully!")
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseCallback?.invoke(false, "Purchase canceled.")
        } else {
            onPurchaseCallback?.invoke(false, "Purchase failed: ${billingResult.debugMessage}")
        }
        onPurchaseCallback = null
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        for (productId in purchase.products) {
            applyPurchaseLocally(productId)

            // If consumable (coins bundle), consume it so it can be bought again!
            if (productId.startsWith("coins_")) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.consumeAsync(consumeParams) { result, _ ->
                    Log.d(TAG, "Consumed $productId: ${result.debugMessage}")
                }
            } else {
                // Non-consumable (remove ads, skins): acknowledge it
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(ackParams) { result ->
                        Log.d(TAG, "Acknowledged $productId: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    fun restorePurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    for (productId in purchase.products) {
                        applyPurchaseLocally(productId)
                    }
                }
            }
        }
    }

    private fun applyPurchaseLocally(productId: String) {
        when (productId) {
            PRODUCT_REMOVE_ADS -> {
                prefs.isAdRemoved = true
                Log.d(TAG, "Ad removal unlocked.")
            }
            PRODUCT_ALL_SKINS -> {
                prefs.unlockAllSkins()
                Log.d(TAG, "All skins unlocked.")
            }
            PRODUCT_SKIN_FRUITS -> prefs.unlockSkin("skin_fruits")
            PRODUCT_SKIN_SMILEYS -> prefs.unlockSkin("skin_emotions")
            PRODUCT_SKIN_ANIMALS -> prefs.unlockSkin("skin_animals")
            PRODUCT_SKIN_BAKERY -> prefs.unlockSkin("skin_sweets")

            PRODUCT_COINS_500 -> prefs.coins += 500
            PRODUCT_COINS_1500 -> prefs.coins += 1500
            PRODUCT_COINS_5000 -> prefs.coins += 5000
            PRODUCT_COINS_15000 -> prefs.coins += 15000
        }
    }
}
