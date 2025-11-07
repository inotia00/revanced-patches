package com.liskovsoft.youtubeapi.app.potokennp2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.MainThread
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.ResourceUtils.getRawResource
import app.revanced.extension.shared.utils.Utils
import com.liskovsoft.sharedutils.okhttp.OkHttpManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class PoTokenWebView private constructor(
    context: Context = Utils.getContext(),
    private var onInitDone: () -> Unit
) : PoTokenGenerator {
    private val webView = WebView(context)
    private val poTokenEmitters = mutableListOf<Pair<String, (String) -> Unit>>()
    private var expirationMs: Long = -1
    var initError: Throwable? = null

    //region Initialization
    init {
        val webViewSettings = webView.settings
        //noinspection SetJavaScriptEnabled we want to use JavaScript!
        webViewSettings.javaScriptEnabled = true
        // MOD: fix AbstractMethodError (Android 8/9)
        //if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
        //    WebSettingsCompat.setSafeBrowsingEnabled(webViewSettings, false)
        //}
        if (Utils.isSDKAbove(26)) {
            webViewSettings.safeBrowsingEnabled = false
        }

        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true // the WebView does not need internet access

        // so that we can run async functions and get back the result
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    // There should not be any uncaught errors while executing the code, because
                    // everything that can fail is guarded by try-catch. Therefore, this likely
                    // indicates that there was a syntax error in the code, i.e. the WebView only
                    // supports a really old version of JS.

                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    Logger.printException { "This WebView implementation is broken: $fmt" }

                    // TODO: not needed anymore?
                    //isBroken = true

                    // Next line cause crashes
                    onInitializationErrorCloseAndCancel(BadWebViewException(fmt))
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    /**
     * Must be called right after instantiating [PoTokenWebView] to perform the actual
     * initialization. This will asynchronously go through all the steps needed to load BotGuard,
     * run it, and obtain an `integrityToken`.
     */
    private fun loadHtmlAndObtainBotguard() {
        Logger.printDebug { "loadHtmlAndObtainBotguard() called" }

        val html = getRawResource("po_token")

        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html.replaceFirst(
                "</script>",
                // calls downloadAndRunBotguard() when the page has finished loading
                "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
            ),
            "text/html",
            "utf-8",
            null,
        )
    }

    /**
     * Called during initialization by the JavaScript snippet appended to the HTML page content in
     * [loadHtmlAndObtainBotguard] after the WebView content has been loaded.
     */
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Logger.printDebug { "downloadAndRunBotguard() called" }

        val responseBody = makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/Create",
            "[ \"$REQUEST_KEY\" ]",
        ) ?: return

        val parsedChallengeData = parseChallengeData(responseBody)

        runOnMainThread {
            webView.evaluateJavascript(
                """try {
                    data = $parsedChallengeData
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                }""",
                null
            )
        }
    }

    /**
     * Called during initialization by the JavaScript snippets from either
     * [downloadAndRunBotguard] or [onRunBotguardResult].
     */
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Logger.printException { "onJsInitializationError: $error" }
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    /**
     * Called during initialization by the JavaScript snippet from [downloadAndRunBotguard] after
     * obtaining the BotGuard execution output [botguardResponse].
     */
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Logger.printDebug { "botguardResponse: $botguardResponse" }

        val responseBody = makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/GenerateIT",
            "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]",
        ) ?: return

        Logger.printDebug { "GenerateIT response: $responseBody" }
        val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)

        // MOD: backport Instant.now().plusSeconds
        // leave 10 minutes of margin just to be sure
        //expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds - 600)
        expirationMs = System.currentTimeMillis() + ((expirationTimeInSeconds - 600) * 1_000)

        runOnMainThread {
            webView.evaluateJavascript(
                "this.integrityToken = $integrityToken"
            ) {
                Logger.printDebug { "initialization finished, expiration=${expirationTimeInSeconds}s" }
                onInitDone()
            }
        }
    }
    //endregion

    //region Obtaining poTokens
    override fun generatePoToken(identifier: String): String {
        Logger.printDebug { "generatePoToken() called with identifier $identifier" }
        val latch = CountDownLatch(1)
        lateinit var pot: String

        addPoTokenEmitter(identifier) {
            pot = it
        }

        val u8Identifier = stringToU8(identifier)

        runOnMainThread {
            webView.evaluateJavascript(
                """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
            ) { latch.countDown() }
        }

        latch.await()

        initError?.let { throw it }

        return pot
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] when an error occurs in calling the
     * JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        Logger.printDebug { "onObtainPoTokenError: $error" }
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] with the original identifier and the
     * result of the JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        Logger.printDebug { "Generated poToken (before decoding): identifier=$identifier poTokenU8=$poTokenU8" }
        val poToken = u8ToBase64(poTokenU8)

        Logger.printDebug { "Generated poToken: identifier=$identifier poToken=$poToken" }
        popPoTokenEmitter(identifier)?.invoke(poToken)
    }

    override fun isExpired(): Boolean {
        // MOD: java.time backport
        //return Instant.now().isAfter(expirationInstant)
        return System.currentTimeMillis() > expirationMs
    }

    //endregion

    //region Handling multiple emitters
    /**
     * Adds the ([identifier], [emitter]) pair to the [poTokenEmitters] list. This makes it so that
     * multiple poToken requests can be generated invparallel, and the results will be notified to
     * the right emitters.
     */
    private fun addPoTokenEmitter(identifier: String, emitter: (String) -> Unit) {
        synchronized(poTokenEmitters) {
            poTokenEmitters.add(Pair(identifier, emitter))
        }
    }

    /**
     * Extracts and removes from the [poTokenEmitters] list a [SingleEmitter] based on its
     * [identifier]. The emitter is supposed to be used immediately after to either signal a success
     * or an error.
     */
    private fun popPoTokenEmitter(identifier: String): ((String) -> Unit)? {
        return synchronized(poTokenEmitters) {
            poTokenEmitters.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenEmitters.removeAt(it).second
            }
        }
    }

    /**
     * Clears [poTokenEmitters] and returns its previous contents. The emitters are supposed to be
     * used immediately after to either signal a success or an error.
     */
    private fun popAllPoTokenEmitters(): List<Pair<String, (String) -> Unit>> {
        return synchronized(poTokenEmitters) {
            val result = poTokenEmitters.toList()
            poTokenEmitters.clear()
            result
        }
    }
    //endregion

    //region Utils
    /**
     * Makes a POST request to [url] with the given [data] by setting the correct headers. Calls
     * [onInitializationErrorCloseAndCancel] in case of any network errors and also if the response
     * does not have HTTP code 200, therefore this is supposed to be used only during
     * initialization. Calls [handleResponseBody] with the response body if the response is
     * successful. The request is performed in the background and a disposable is added to
     * [disposables].
     */
    private fun makeBotguardServiceRequest(
        url: String,
        data: String
    ): String? {
        val response = OkHttpManager.instance().doPostRequest(
            url,
            mapOf(
                // replace the downloader user agent
                "User-Agent" to USER_AGENT,
                "Accept" to "application/json",
                "Content-Type" to "application/json+protobuf",
                "X-Goog-Api-Key" to GOOGLE_API_KEY,
                "X-User-Agent" to "grpc-web-javascript/0.1",
            ),
            data,
            null
        )

        val httpCode = response.code

        if (httpCode != 200) {
            onInitializationErrorCloseAndCancel(PoTokenException("Invalid response code: $httpCode"))
        }

        if (response.body == null) {
            onInitializationErrorCloseAndCancel(PoTokenException("Response body is empty. Response code: $httpCode"))
        }

        return response.body?.string()
    }

    /**
     * Handles any error happening during initialization, releasing resources and sending the error
     * to [generatorEmitter].
     */
    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        initError = error
        popAllPoTokenEmitters()
        runOnMainThread {
            close()
            // throw error
            onInitDone()
        }
    }

    /**
     * Releases all [webView] and [disposables] resources.
     */
    @MainThread
    override fun close() {
        webView.clearHistory()
        // clears RAM cache and disk cache (globally for all WebViews)
        webView.clearCache(true)

        // ensures that the WebView isn't doing anything when destroying it
        webView.loadUrl("about:blank")

        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }
    //endregion

    companion object : PoTokenGenerator.Factory {
        // Public API key used by BotGuard, which has been got by looking at BotGuard requests
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw" // NOSONAR
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"

        override fun newPoTokenGenerator(context: Context): PoTokenGenerator {
            if (hasThermalServiceBug(context)) {
                throw BadWebViewException("ThermalService isn't available")
            }

            if (hasUsbServiceBug(context)) {
                throw BadWebViewException("Usb service isn't available")
            }

            val latch = CountDownLatch(1)

            lateinit var potWv: PoTokenWebView
            var initError: Throwable? = null

            runOnMainThread {
                potWv = try {
                    PoTokenWebView(context) { latch.countDown() }
                } catch (e: Throwable) {
                    initError = BadWebViewException("${e::class.simpleName}: ${e.message}")
                    latch.countDown()
                    return@runOnMainThread
                }
                potWv.loadHtmlAndObtainBotguard()
            }

            latch.await(20, TimeUnit.SECONDS)

            initError?.let { throw it }
            potWv.initError?.let { throw it }

            return potWv
        }

        /**
         * Runs [runnable] on the main thread using `Handler(Looper.getMainLooper()).post()`, and
         * if the `post` fails emits an error on [emitterIfPostFails].
         */
        private fun runOnMainThread(
            runnable: Runnable
        ) {
            if (!Handler(Looper.getMainLooper()).post(runnable)) {
                throw PoTokenException("Could not run on main thread")
            }
        }
    }
}
