package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
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
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {

    private const val TAG = "AdMobManager"
    private const val INTERSTITIAL_COOLDOWN_MS = 180_000L

    private val initialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    @Volatile
    private var isLoadingInterstitial = false

    @Volatile
    private var lastInterstitialShownAtMs = 0L

    private fun isRobolectric(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    fun isEnabled(): Boolean = BuildConfig.ADMOB_ENABLED && !isRobolectric()

    fun initialize(context: Context) {
        if (!isEnabled()) return
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        runCatching {
            MobileAds.initialize(appContext) {
                preloadInterstitial(appContext)
            }
        }.onFailure { err ->
            initialized.set(false)
            Log.w(TAG, "MobileAds initialize failed", err)
        }
    }

    fun preloadInterstitial(context: Context) {
        if (!isEnabled()) return
        val appContext = context.applicationContext

        val loadRunnable = Runnable {
            if (interstitialAd != null || isLoadingInterstitial) return@Runnable
            isLoadingInterstitial = true

            runCatching {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    appContext,
                    BuildConfig.ADMOB_INTERSTITIAL_ID,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isLoadingInterstitial = false
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            interstitialAd = null
                            isLoadingInterstitial = false
                            Log.d(TAG, "Interstitial failed to load: ${loadError.message}")
                        }
                    }
                )
            }.onFailure { err ->
                isLoadingInterstitial = false
                Log.w(TAG, "Interstitial load invocation error", err)
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            loadRunnable.run()
        } else {
            mainHandler.post(loadRunnable)
        }
    }

    fun showInterstitialIfReady(
        activity: Activity?,
        onFinished: () -> Unit = {}
    ) {
        if (!isEnabled() || activity == null || activity.isFinishing || activity.isDestroyed) {
            onFinished()
            return
        }

        val now = System.currentTimeMillis()
        if (lastInterstitialShownAtMs > 0L && now - lastInterstitialShownAtMs < INTERSTITIAL_COOLDOWN_MS) {
            preloadInterstitial(activity.applicationContext)
            onFinished()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity.applicationContext)
            onFinished()
            return
        }

        val appContext = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastInterstitialShownAtMs = System.currentTimeMillis()
                preloadInterstitial(appContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preloadInterstitial(appContext)
                onFinished()
            }

            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
                lastInterstitialShownAtMs = System.currentTimeMillis()
            }
        }

        runCatching {
            ad.show(activity)
        }.onFailure {
            interstitialAd = null
            preloadInterstitial(appContext)
            onFinished()
        }
    }
}

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible || !AdMobManager.isEnabled() || LocalInspectionMode.current) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val cardShape = RoundedCornerShape(18.dp)
    val cardBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.56f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF6FCFD).copy(alpha = 0.74f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f)
            )
        )
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            .clip(cardShape)
            .background(brush = cardBrush, shape = cardShape)
            .testTag("admob_banner_bar"),
        shape = cardShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val adWidthDp = maxWidth.value.toInt().coerceAtLeast(300)
            val adView = remember(context, adWidthDp) {
                runCatching {
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        adWidthDp
                    )
                    AdView(context).apply {
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        setAdSize(adaptiveSize)
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.d("AdMobBanner", "Banner failed to load: ${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }.getOrNull()
            }

            if (adView != null) {
                DisposableEffect(lifecycleOwner, adView) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> runCatching { adView.resume() }
                            Lifecycle.Event.ON_PAUSE -> runCatching { adView.pause() }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        runCatching { adView.destroy() }
                    }
                }

                AndroidView(
                    factory = { adView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(modifier = Modifier.height(0.dp))
            }
        }
    }
}
