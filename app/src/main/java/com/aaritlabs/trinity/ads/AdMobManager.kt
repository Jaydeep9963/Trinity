package com.aaritlabs.trinity.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.data.GamePreferences
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManager private constructor(private val appContext: Context) {

    private val prefs = GamePreferences(appContext)
    private var rewardedBoosterAd: RewardedAd? = null
    private var rewardedCoinsAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingRewardedBooster = false
    private var isLoadingRewardedCoins = false
    private var isLoadingInterstitial = false

    companion object {
        private const val TAG = "AdMobManager"

        // Official Google Sample Test Ad Unit IDs for instant fallback
        private const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

        @Volatile
        private var instance: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager {
            return instance ?: synchronized(this) {
                instance ?: AdMobManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun initialize() {
        try {
            MobileAds.initialize(appContext) {
                Log.d(TAG, "Google Mobile Ads initialized successfully.")
                loadRewardedBoosterAd()
                loadRewardedCoinsAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed: ${e.message}")
        }
    }

    fun loadBannerAd(adView: AdView, container: ViewGroup? = null) {
        if (prefs.isAdRemoved) {
            adView.visibility = View.GONE
            container?.visibility = View.GONE
            return
        }
        adView.visibility = View.VISIBLE
        container?.visibility = View.VISIBLE

        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner failed with production ID (${error.message}). Loading Google test banner fallback.")
                if (container != null) {
                    try {
                        container.removeAllViews()
                        val testAdView = AdView(container.context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = TEST_BANNER_AD_ID
                        }
                        container.addView(testAdView)
                        testAdView.loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load test banner: ${e.message}")
                    }
                }
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadRewardedBoosterAd() {
        if (isLoadingRewardedBooster || rewardedBoosterAd != null) return
        isLoadingRewardedBooster = true

        val prodAdUnitId = appContext.getString(R.string.admob_rewarded_booster_ad_id)
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(appContext, prodAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedBoosterAd = ad
                isLoadingRewardedBooster = false
                Log.d(TAG, "Production rewarded booster ad loaded successfully.")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Production rewarded booster ad failed (${error.message}). Retrying with Google Test Rewarded unit.")
                RewardedAd.load(appContext, TEST_REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(testAd: RewardedAd) {
                        rewardedBoosterAd = testAd
                        isLoadingRewardedBooster = false
                        Log.d(TAG, "Fallback Google test rewarded booster ad loaded.")
                    }

                    override fun onAdFailedToLoad(err: LoadAdError) {
                        rewardedBoosterAd = null
                        isLoadingRewardedBooster = false
                        Log.w(TAG, "Fallback Google test ad failed: ${err.message}")
                    }
                })
            }
        })
    }

    fun loadRewardedCoinsAd() {
        if (isLoadingRewardedCoins || rewardedCoinsAd != null) return
        isLoadingRewardedCoins = true

        val prodAdUnitId = appContext.getString(R.string.admob_rewarded_coins_ad_id)
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(appContext, prodAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedCoinsAd = ad
                isLoadingRewardedCoins = false
                Log.d(TAG, "Production rewarded coins ad loaded successfully.")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Production rewarded coins ad failed (${error.message}). Retrying with Google Test Rewarded unit.")
                RewardedAd.load(appContext, TEST_REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(testAd: RewardedAd) {
                        rewardedCoinsAd = testAd
                        isLoadingRewardedCoins = false
                        Log.d(TAG, "Fallback Google test rewarded coins ad loaded.")
                    }

                    override fun onAdFailedToLoad(err: LoadAdError) {
                        rewardedCoinsAd = null
                        isLoadingRewardedCoins = false
                        Log.w(TAG, "Fallback Google test ad failed: ${err.message}")
                    }
                })
            }
        })
    }

    fun showRewardedBoosterAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onFallbackSimulation: (() -> Unit)? = null
    ) {
        val ad = rewardedBoosterAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedBoosterAd = null
                    loadRewardedBoosterAd()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedBoosterAd = null
                    loadRewardedBoosterAd()
                    onFallbackSimulation?.invoke() ?: onAdClosed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned booster reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            }
        } else {
            // Preloaded ad not ready yet: try immediate test ad load on the fly
            Log.d(TAG, "Preloaded booster ad not ready. Attempting on-the-fly Google test ad load.")
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(activity, TEST_REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(testAd: RewardedAd) {
                    testAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadRewardedBoosterAd()
                            onAdClosed()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            loadRewardedBoosterAd()
                            onFallbackSimulation?.invoke() ?: onAdClosed()
                        }
                    }
                    testAd.show(activity) {
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "On-the-fly test ad failed (${error.message}). Running visual fallback simulation.")
                    loadRewardedBoosterAd()
                    onFallbackSimulation?.invoke() ?: run {
                        onRewardEarned()
                        onAdClosed()
                    }
                }
            })
        }
    }

    fun showRewardedCoinsAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onFallbackSimulation: (() -> Unit)? = null
    ) {
        val ad = rewardedCoinsAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedCoinsAd = null
                    loadRewardedCoinsAd()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedCoinsAd = null
                    loadRewardedCoinsAd()
                    onFallbackSimulation?.invoke() ?: onAdClosed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned coins reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            }
        } else {
            // Preloaded ad not ready yet: try immediate test ad load on the fly
            Log.d(TAG, "Preloaded coins ad not ready. Attempting on-the-fly Google test ad load.")
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(activity, TEST_REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(testAd: RewardedAd) {
                    testAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadRewardedCoinsAd()
                            onAdClosed()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            loadRewardedCoinsAd()
                            onFallbackSimulation?.invoke() ?: onAdClosed()
                        }
                    }
                    testAd.show(activity) {
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "On-the-fly test coins ad failed (${error.message}). Running visual fallback simulation.")
                    loadRewardedCoinsAd()
                    onFallbackSimulation?.invoke() ?: run {
                        onRewardEarned()
                        onAdClosed()
                    }
                }
            })
        }
    }

    // Generic fallback for any other caller
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onFallbackSimulation: (() -> Unit)? = null
    ) {
        showRewardedBoosterAd(activity, onRewardEarned, onAdClosed, onFallbackSimulation)
    }

    fun loadInterstitialAd() {
        if (prefs.isAdRemoved || isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true

        val prodAdUnitId = appContext.getString(R.string.admob_interstitial_ad_id)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(appContext, prodAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoadingInterstitial = false
                Log.d(TAG, "Production interstitial ad loaded successfully.")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Production interstitial ad failed (${error.message}). Retrying with Google Test Interstitial unit.")
                InterstitialAd.load(appContext, TEST_INTERSTITIAL_AD_ID, adRequest, object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(testAd: InterstitialAd) {
                        interstitialAd = testAd
                        isLoadingInterstitial = false
                        Log.d(TAG, "Fallback Google test interstitial ad loaded.")
                    }

                    override fun onAdFailedToLoad(err: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                        Log.w(TAG, "Fallback Google test interstitial ad failed: ${err.message}")
                    }
                })
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onClosed: () -> Unit) {
        if (prefs.isAdRemoved) {
            onClosed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onClosed()
                }
            }
            ad.show(activity)
        } else {
            // Attempt on-the-fly test interstitial if available, else continue immediately
            Log.d(TAG, "Interstitial ad not ready. Attempting on-the-fly Google test interstitial.")
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(activity, TEST_INTERSTITIAL_AD_ID, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(testAd: InterstitialAd) {
                    testAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadInterstitialAd()
                            onClosed()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            loadInterstitialAd()
                            onClosed()
                        }
                    }
                    testAd.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadInterstitialAd()
                    onClosed()
                }
            })
        }
    }
}
