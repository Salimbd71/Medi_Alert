package com.slm.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.slm.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {

    private const val TAG = "AdMobManager"
    private const val INTERSTITIAL_COOLDOWN_MS = 180_000L

    private val initialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

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
        val initBlock = Runnable {
            runCatching {
                MobileAds.initialize(appContext) {
                    _isSdkInitialized.value = true
                    preloadInterstitial(appContext)
                }
            }.onFailure { err ->
                initialized.set(false)
                _isSdkInitialized.value = false
                Log.w(TAG, "MobileAds initialize failed", err)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initBlock.run()
        } else {
            mainHandler.post(initBlock)
        }
    }

    fun preloadInterstitial(context: Context) {
        if (!isEnabled() || !_isSdkInitialized.value) return
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
    visible: Boolean = true,
    inlineAdaptive: Boolean = false,
    asCard: Boolean = true
) {
    if (!visible || !AdMobManager.isEnabled() || LocalInspectionMode.current) {
        return
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f

    if (asCard) {
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
                    Color.White.copy(alpha = 0.82f),
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
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
                .clip(cardShape)
                .background(brush = cardBrush, shape = cardShape)
                .testTag(if (inlineAdaptive) "admob_inline_banner_bar" else "admob_banner_bar"),
            shape = cardShape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            AdMobBannerContent(
                inlineAdaptive = inlineAdaptive,
                clipToRounded = true,
                horizontalPadding = 8.dp,
                verticalPadding = 6.dp
            )
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag(if (inlineAdaptive) "admob_inline_banner_bar" else "admob_banner_bar"),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            )
        ) {
            AdMobBannerContent(
                inlineAdaptive = inlineAdaptive,
                clipToRounded = false,
                horizontalPadding = 4.dp,
                verticalPadding = 4.dp
            )
        }
    }
}

@Composable
private fun AdMobBannerContent(
    inlineAdaptive: Boolean,
    clipToRounded: Boolean,
    horizontalPadding: Dp,
    verticalPadding: Dp
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sdkReady by AdMobManager.isSdkInitialized.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        val adWidthDp = maxWidth.value.toInt().coerceAtLeast(300)
        val adView = remember(context, adWidthDp, inlineAdaptive) {
            runCatching {
                val adaptiveSize = if (inlineAdaptive) {
                    AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                        context,
                        adWidthDp
                    )
                } else {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        adWidthDp
                    )
                }
                AdView(context).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_ID
                    setAdSize(adaptiveSize)
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.d("AdMobBanner", "Banner failed to load: ${error.message}")
                        }
                    }
                }
            }.getOrNull()
        }

        if (adView != null) {
            var isAttached by remember(adView) { mutableStateOf(adView.isAttachedToWindow) }
            var hasLoaded by remember(adView) { mutableStateOf(false) }

            DisposableEffect(lifecycleOwner, adView) {
                val attachListener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        isAttached = true
                        if (hasLoaded) {
                            runCatching { adView.resume() }
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        isAttached = false
                        runCatching { adView.pause() }
                    }
                }
                adView.addOnAttachStateChangeListener(attachListener)
                if (adView.isAttachedToWindow) {
                    isAttached = true
                }

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
                    adView.removeOnAttachStateChangeListener(attachListener)
                    runCatching {
                        adView.pause()
                        adView.destroy()
                    }
                }
            }

            LaunchedEffect(adView, sdkReady, isAttached) {
                if (sdkReady && isAttached && !hasLoaded) {
                    hasLoaded = true
                    runCatching {
                        adView.loadAd(AdRequest.Builder().build())
                    }
                }
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (clipToRounded) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier)
            )
        } else {
            Box(modifier = Modifier.height(0.dp))
        }
    }
}
