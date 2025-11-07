package com.liskovsoft.youtubeapi.app.potokennp2

import android.os.Handler
import android.os.Looper
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import com.liskovsoft.sharedutils.helpers.DeviceHelpers
import com.liskovsoft.youtubeapi.app.potokennp2.PoTokenProviderImpl.webPoTokenGenerator
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenProvider
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenResult
import com.liskovsoft.youtubeapi.app.potokennp2.visitor.VisitorService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object PoTokenProviderImpl : PoTokenProvider {
    private val mContext by lazy { Utils.getContext() }
    private val webViewSupported by lazy { DeviceHelpers.isWebViewSupported() }
    private var webViewBadImpl = false // whether the system has a bad WebView implementation

    private object WebPoTokenGenLock

    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (!isWebPotSupported) {
            return null
        }

        try {
            return getWebClientPoToken(videoId = videoId, forceRecreate = false)
        } catch (e: RuntimeException) {
            // RxJava's Single wraps exceptions into RuntimeErrors, so we need to unwrap them here
            when (val cause = e.cause) {
                is BadWebViewException -> {
                    Logger.printException(
                        { "Could not obtain poToken because WebView is broken" },
                        e
                    )
                    webViewBadImpl = true
                    return null
                }

                null -> throw e
                else -> throw cause // includes PoTokenException
            }
        }
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenGenerator.generatePoToken] was called
     */
    private fun getWebClientPoToken(videoId: String, forceRecreate: Boolean): PoTokenResult {
        // just a helper class since Kotlin does not have builtin support for 4-tuples
        data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

        val (poTokenGenerator, visitorData, streamingPot, hasBeenRecreated) =
            synchronized(WebPoTokenGenLock) {
                val shouldRecreate =
                    webPoTokenGenerator == null || webPoTokenVisitorData == null || webPoTokenStreamingPot == null ||
                            forceRecreate || webPoTokenGenerator!!.isExpired()

                if (shouldRecreate) {
                    // MOD: my visitor data
                    //webPoTokenVisitorData = AppService.instance().visitorData
                    webPoTokenVisitorData = VisitorService.getVisitorData()

                    val latch = if (webPoTokenGenerator != null) CountDownLatch(1) else null

                    // close the current webPoTokenGenerator on the main thread
                    webPoTokenGenerator?.let {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                it.close()
                            } finally {
                                latch?.countDown()
                            }
                        }
                    }

                    latch?.await(3, TimeUnit.SECONDS)

                    // create a new webPoTokenGenerator
                    webPoTokenGenerator = PoTokenWebView
                        .newPoTokenGenerator(mContext)

                    // The streaming poToken needs to be generated exactly once before generating
                    // any other (player) tokens.
                    webPoTokenStreamingPot = webPoTokenGenerator!!
                        .generatePoToken(webPoTokenVisitorData!!)
                }

                return@synchronized Quadruple(
                    webPoTokenGenerator!!,
                    webPoTokenVisitorData!!,
                    webPoTokenStreamingPot!!,
                    shouldRecreate
                )
            }

        val playerPot = try {
            // Not using synchronized here, since poTokenGenerator would be able to generate
            // multiple poTokens in parallel if needed. The only important thing is for exactly one
            // visitorData/streaming poToken to be generated before anything else.
            if (videoId.isEmpty()) "" else poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                // the poTokenGenerator has just been recreated (and possibly this is already the
                // second time we try), so there is likely nothing we can do
                throw throwable
            } else {
                // retry, this time recreating the [webPoTokenGenerator] from scratch;
                // this might happen for example if NewPipe goes in the background and the WebView
                // content is lost
                Logger.printException({ "Failed to obtain poToken, retrying" }, throwable)

                return getWebClientPoToken(videoId = videoId, forceRecreate = true)
            }
        }

        Logger.printDebug {
            "poToken for $videoId: playerPot=$playerPot, " +
                    "streamingPot=$streamingPot, visitor_data=$visitorData"
        }

        return PoTokenResult(videoId, visitorData, playerPot, streamingPot)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null

    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null

    override fun isWebPotExpired() = isWebPotSupported && webPoTokenGenerator?.isExpired() ?: true

    override fun isWebPotSupported() = webViewSupported && !webViewBadImpl

    fun resetCache() {
        webPoTokenVisitorData = null
        webPoTokenStreamingPot = null
    }
}
