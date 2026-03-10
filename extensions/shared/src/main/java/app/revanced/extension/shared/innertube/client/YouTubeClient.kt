package app.revanced.extension.shared.innertube.client

import android.os.Build
import app.revanced.extension.shared.innertube.utils.J2V8Support.supportJ2V8
import app.revanced.extension.shared.patches.AppCheckPatch.IS_YOUTUBE
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.PackageUtils
import org.apache.commons.lang3.ArrayUtils
import java.util.Locale

/**
 * Used to fetch streaming data.
 */
@Suppress("ConstantLocale", "unused")
object YouTubeClient {
    private const val CLIENT_SCREEN_WATCH = "WATCH"
    private const val CLIENT_SCREEN_EMBED = "EMBED"
    private const val CLIENT_PLATFORM_DESKTOP = "DESKTOP"
    private const val CLIENT_PLATFORM_GAME_CONSOLE = "GAME_CONSOLE"
    private const val CLIENT_PLATFORM_MOBILE = "MOBILE"
    private const val CLIENT_PLATFORM_TABLET = "TABLET"
    private const val CLIENT_PLATFORM_TV = "TV"

    private const val CLIENT_REFERER_FORMAT_TV = "https://www.youtube.com/tv#/watch?v=%s"
    private const val CLIENT_REFERER_FORMAT_WEB = "https://www.youtube.com/watch?v=%s"
    private const val CLIENT_REFERER_FORMAT_MWEB = "https://m.youtube.com/watch?v=%s"


    // ANDROID
    /**
     * Requires a DroidGuard PoToken (if the user is logged in) to play videos longer than 1:00.
     */
    private const val PACKAGE_NAME_ANDROID = "com.google.android.youtube"
    private val CLIENT_VERSION_ANDROID = PackageUtils.getAppVersionName()
    private val USER_AGENT_ANDROID = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID,
        clientVersion = CLIENT_VERSION_ANDROID,
    )


    // ANDROID (NO SDK)
    /**
     * Video not playable in YouTube: None.
     * Video not playable in YouTube Music (Auth): None.
     * Video not playable in YouTube Music (No Auth): Paid, Movie, Private, Age-restricted.
     * Uses adaptive bitrate.
     */
    private const val CLIENT_VERSION_ANDROID_NO_SDK = "20.05.46"
    private const val DEVICE_MODEL_ANDROID_NO_SDK = ""
    private const val DEVICE_MAKE_ANDROID_NO_SDK = ""
    private val OS_VERSION_ANDROID_NO_SDK = Build.VERSION.RELEASE
    private val ANDROID_SDK_VERSION_ANDROID_NO_SDK: String? = null
    private val USER_AGENT_ANDROID_NO_SDK =
        "$PACKAGE_NAME_ANDROID/$CLIENT_VERSION_ANDROID_NO_SDK (Linux; U; Android $OS_VERSION_ANDROID_NO_SDK) gzip"


    // ANDROID_MUSIC (NO SDK)
    /**
     * Video not playable in YouTube: All videos (This client requires login, but cannot log in with YouTube's access token).
     * Video not playable in YouTube Music: None.
     * Uses non adaptive bitrate.
     */
    private const val PACKAGE_NAME_ANDROID_MUSIC = "com.google.android.apps.youtube.music"
    private const val CLIENT_VERSION_ANDROID_MUSIC_NO_SDK = "7.12.52"
    private const val DEVICE_MODEL_ANDROID_MUSIC_NO_SDK = ""
    private const val DEVICE_MAKE_ANDROID_MUSIC_NO_SDK = ""
    private val OS_VERSION_ANDROID_MUSIC_NO_SDK = Build.VERSION.RELEASE
    private val ANDROID_SDK_VERSION_ANDROID_MUSIC_NO_SDK: String? = null
    private val USER_AGENT_ANDROID_MUSIC_NO_SDK =
        "$PACKAGE_NAME_ANDROID_MUSIC/$CLIENT_VERSION_ANDROID_MUSIC_NO_SDK (Linux; U; Android $OS_VERSION_ANDROID_MUSIC_NO_SDK) gzip"


    // ANDROID VR
    /**
     * Video not playable (Auth): Kids.
     * Video not playable (No Auth): Kids, Paid, Movie, Private, Age-restricted.
     * Uses non adaptive bitrate.
     *
     * Package name for YouTube VR (Google DayDream): com.google.android.apps.youtube.vr (Deprecated)
     * Package name for YouTube VR (Meta Quests): com.google.android.apps.youtube.vr.oculus
     * Package name for YouTube VR (ByteDance Pico): com.google.android.apps.youtube.vr.pico
     * Package name for YouTube XR (Samsung Galaxy XR): com.google.android.apps.youtube.xr
     */
    private const val PACKAGE_NAME_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus"

    /**
     * The hardcoded client version of the Android VR app used for InnerTube requests with this client.
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App Store page of the YouTube app](https://www.meta.com/en-us/experiences/2002317119880945/),
     * in the `Additional details` section.
     */
    private val CLIENT_VERSION_ANDROID_VR = if (useAV1())
        // Lowest version that supports AV1.
        // According to the changelog, only Quest 3 supports the AV1 codec in this version.
        // Cronet version: 122.0.6238.3
        "1.54.20"
    else
        // Cronet version: 113.0.5672.24
        "1.47.48"

    private val DEVICE_MODEL_ANDROID_VR = if (useAV1())
        // https://dumps.tadiphone.dev/dumps/oculus/eureka
        "Quest 3"
    else
        // https://dumps.tadiphone.dev/dumps/oculus/monterey
        "Quest"
    private const val DEVICE_MAKE_ANDROID_VR = "Oculus"
    private val OS_VERSION_ANDROID_VR = if (useAV1())
        "14"
    else
        "10"
    private val ANDROID_SDK_VERSION_ANDROID_VR = if (useAV1())
        "34"
    else
        "29"
    private val BUILD_ID_ANDROID_VR = if (useAV1())
        "UP1A.231005.007.A1"
    else
        "QQ3A.200805.001"

    private val USER_AGENT_ANDROID_VR = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID_VR,
        clientVersion = CLIENT_VERSION_ANDROID_VR,
        osVersion = OS_VERSION_ANDROID_VR,
        deviceModel = DEVICE_MODEL_ANDROID_VR,
        buildId = BUILD_ID_ANDROID_VR
    )


    // ANDROID CREATOR
    /**
     * Video not playable: Livestream.
     * Uses non adaptive bitrate.
     * AV1 codec and HDR codec are not available, and the maximum resolution is 720p.
     * 360° VR immersive mode is not available.
     */
    private const val PACKAGE_NAME_ANDROID_CREATOR = "com.google.android.apps.youtube.creator"
    private const val CLIENT_VERSION_ANDROID_CREATOR = "24.01.000"

    /**
     * The device machine id for the Google Pixel 7a.
     * See [this GitLab](https://dumps.tadiphone.dev/dumps/google/lynx) for more information.
     */
    private const val DEVICE_MODEL_ANDROID_CREATOR = "Pixel 7a"
    private const val DEVICE_MAKE_ANDROID_CREATOR = "Google"
    private const val OS_VERSION_ANDROID_CREATOR = "13"
    private const val ANDROID_SDK_VERSION_ANDROID_CREATOR = "33"
    private const val BUILD_ID_ANDROID_CREATOR = "TQ3A.230901.001.C3" // Pixel 7a Verizon

    private val USER_AGENT_ANDROID_CREATOR = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID_CREATOR,
        clientVersion = CLIENT_VERSION_ANDROID_CREATOR,
        osVersion = OS_VERSION_ANDROID_CREATOR,
        deviceModel = DEVICE_MODEL_ANDROID_CREATOR,
        buildId = BUILD_ID_ANDROID_CREATOR,
    )


    // VISION OS
    private const val CLIENT_VERSION_VISIONOS = "0.1"
    private const val DEVICE_MAKE_VISIONOS = "Apple"
    private const val DEVICE_MODEL_VISIONOS = "RealityDevice14,1"
    private const val OS_NAME_VISIONOS = "visionOS"
    private const val OS_VERSION_VISIONS = "1.3.21O771"
    private const val USER_AGENT_VISIONOS =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"


    // TVHTML5
    /**
     * Video not playable: None.
     * 360° VR immersive mode is not available.
     */
    private const val CLIENT_VERSION_TVHTML5 = "7.20251217.19.00"
    private const val USER_AGENT_TVHTML5 =
        "Mozilla/5.0 (SMART-TV; Linux; Tizen 8.0) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/7.0 Chrome/108.0.5359.1 TV Safari/537.36"


    // TVHTML5 (Downgraded)
    /**
     * Same as TVHTML5, but can play SABR format-only videos.
     * See: https://github.com/yt-dlp/yt-dlp/pull/14887.
     *
     * Available version
     * ===============
     * '5.20150304'
     * '5.20160729'
     * '6.20180913'
     */
    private const val CLIENT_VERSION_TVHTML5_LEGACY = "5.20150304"
    /**
     * authenticatedConfig.flags.attest_botguard_on_tvhtml5: false.
     */
    private const val USER_AGENT_TVHTML5_LEGACY =
        "Mozilla/5.0 (Linux mipsel) Cobalt/9.28152-debug (unlike Gecko) Starboard/4"


    // TVHTML5 SIMPLY
    /**
     * Video not playable: None.
     * 360° VR immersive mode is not available.
     */
    private const val CLIENT_VERSION_TVHTML5_SIMPLY = "1.1"
    /**
     * authenticatedConfig.flags.attest_botguard_on_tvhtml5: false.
     */
    private const val USER_AGENT_TVHTML5_SIMPLY =
        "Mozilla/5.0 (PS4; Leanback Shell) Gecko/20100101 Firefox/65.0 LeanbackShell/01.00.01.75 Sony PS4/ (PS4, , no, CH)"


    // TVHTML5 EMBEDDED
    /**
     * Only embeddable videos available.
     * 360° VR immersive mode is not available.
     */
    private const val CLIENT_VERSION_TVHTML5_EMBEDDED = "2.0"


    // WEB (Downgraded)
    /**
     * Same as WEB, but for some reason SABR is not applied.
     *
     * Available version
     * ===============
     * '1.20160315'
     * '1.20161001'
     * '1.20170222'
     */
    private const val CLIENT_VERSION_WEB_LEGACY = "1.20160315"
    private const val USER_AGENT_WEB_LEGACY =
        "Mozilla/5.0 (X11; OpenBSD amd64; rv:45.0) Gecko/20100101 Firefox/45.0"


    // MWEB
    /**
     * Video not playable: Paid, Movie, Private, Age-restricted.
     * 360° VR immersive mode is not available.
     */
    private const val CLIENT_VERSION_MWEB = "2.20251105.03.00"
    private const val USER_AGENT_MWEB =
        "Mozilla/5.0 (Android 16; Mobile; rv:140.0) Gecko/140.0 Firefox/140.0"


    /**
     * Same format as Android YouTube User-Agent.
     * Example: 'com.google.android.youtube/20.32.35(Linux; U; Android 15; en_US; SM-S928U1 Build/AP3A.240905.015.A2) gzip'
     * Source: https://whatmyuseragent.com/apps/youtube.
     */
    private fun androidUserAgent(
        packageName: String,
        clientVersion: String,
        osVersion: String? = Build.VERSION.RELEASE,
        deviceModel: String? = Build.MODEL,
        buildId: String? = Build.ID,
    ): String =
        "$packageName/$clientVersion(Linux; U; Android $osVersion; ${Locale.getDefault()}; $deviceModel Build/$buildId) gzip"

    private fun useAV1(): Boolean {
        return BaseSettings.SPOOF_STREAMING_DATA_ANDROID_VR_ENABLE_AV1_CODEC.get()
    }

    private fun useJS(): Boolean {
        return supportJ2V8() && BaseSettings.SPOOF_STREAMING_DATA_USE_JS.get()
    }

    @JvmStatic
    fun availableClientTypes(preferredClient: ClientType): Array<ClientType> {
        val availableClientTypes: Array<ClientType> = if (useJS()) {
            if (IS_YOUTUBE) {
                ClientType.CLIENT_ORDER_TO_USE_JS_YOUTUBE
            } else {
                ClientType.CLIENT_ORDER_TO_USE_JS_YOUTUBE_MUSIC
            }
        } else {
            if (IS_YOUTUBE) {
                ClientType.CLIENT_ORDER_TO_USE_YOUTUBE
            } else {
                ClientType.CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC
            }
        }

        if (ArrayUtils.contains(availableClientTypes, preferredClient)) {
            val clientToUse: Array<ClientType?> = arrayOfNulls(availableClientTypes.size)
            clientToUse[0] = preferredClient
            var i = 1
            for (c in availableClientTypes) {
                if (c != preferredClient) {
                    clientToUse[i++] = c
                }
            }
            return clientToUse.filterNotNull().toTypedArray()
        } else {
            BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.resetToDefault()
            return availableClientTypes
        }
    }

    @Suppress("DEPRECATION", "unused")
    enum class ClientType(
        /**
         * [YouTube client type](https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients)
         */
        val id: Int,
        /**
         * Device model, equivalent to [Build.MANUFACTURER] (System property: ro.product.vendor.manufacturer)
         */
        val deviceMake: String = Build.MANUFACTURER,
        /**
         * Device model, equivalent to [Build.MODEL] (System property: ro.product.model)
         */
        val deviceModel: String = Build.MODEL,
        /**
         * Device OS name.
         */
        val osName: String = "Android",
        /**
         * Device OS version, equivalent to [Build.VERSION.RELEASE] (System property: ro.system.build.version.release)
         */
        val osVersion: String = Build.VERSION.RELEASE,
        /**
         * Client user-agent.
         */
        val userAgent: String,
        /**
         * Android SDK version, equivalent to [Build.VERSION.SDK] (System property: ro.build.version.sdk)
         */
        val androidSdkVersion: String? = null,
        /**
         * App version.
         */
        val clientVersion: String,
        /**
         * Client platform enum.
         */
        val clientPlatform: String = CLIENT_PLATFORM_MOBILE,
        /**
         * Client screen enum.
         */
        val clientScreen: String = CLIENT_SCREEN_WATCH,
        /**
         * If the client can access the API logged in.
         * If false, 'Authorization' or 'SessionId' must not be included.
         */
        val supportsCookies: Boolean = true,
        /**
         * Whether it supports multiple audio tracks.
         */
        val supportsMultiAudioTracks: Boolean = false,
        /**
         * Referer of contentPlaybackContext.
         */
        val refererFormat: String? = null,
        /**
         * If the client can only access the API logged in.
         * If true, 'Authorization' or 'SessionId' must be included.
         */
        val requireAuth: Boolean = false,
        /**
         * If the client requires GVS PoToken.
         */
        val requirePoToken: Boolean = false,
        /**
         * The streaming url has an obfuscated 'n' parameter.
         * If true, javascript must be fetched to decrypt the 'n' parameter.
         */
        val requireJS: Boolean = false,
        /**
         * Client name for innertube body.
         */
        val clientName: String,
        /**
         * Friendly name displayed in stats for nerds.
         */
        val friendlyName: String
    ) {
        ANDROID(
            id = 3,
            userAgent = USER_AGENT_ANDROID,
            androidSdkVersion = Build.VERSION.SDK,
            clientVersion = CLIENT_VERSION_ANDROID,
            supportsMultiAudioTracks = true,
            clientName = "ANDROID",
            friendlyName = "Android"
        ),
        ANDROID_NO_SDK(
            id = 3,
            deviceMake = DEVICE_MAKE_ANDROID_NO_SDK,
            deviceModel = DEVICE_MODEL_ANDROID_NO_SDK,
            osVersion = OS_VERSION_ANDROID_NO_SDK,
            userAgent = USER_AGENT_ANDROID_NO_SDK,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_NO_SDK,
            clientVersion = CLIENT_VERSION_ANDROID_NO_SDK,
            supportsMultiAudioTracks = true,
            clientName = "ANDROID",
            friendlyName = "Android No SDK"
        ),
        ANDROID_MUSIC_NO_SDK(
            id = 21,
            deviceMake = DEVICE_MAKE_ANDROID_MUSIC_NO_SDK,
            deviceModel = DEVICE_MODEL_ANDROID_MUSIC_NO_SDK,
            osVersion = OS_VERSION_ANDROID_MUSIC_NO_SDK,
            userAgent = USER_AGENT_ANDROID_MUSIC_NO_SDK,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_MUSIC_NO_SDK,
            clientVersion = CLIENT_VERSION_ANDROID_MUSIC_NO_SDK,
            requireAuth = true,
            clientName = "ANDROID_MUSIC",
            friendlyName = "Android Music No SDK"
        ),
        ANDROID_VR(
            id = 28,
            deviceMake = DEVICE_MAKE_ANDROID_VR,
            deviceModel = DEVICE_MODEL_ANDROID_VR,
            osVersion = OS_VERSION_ANDROID_VR,
            userAgent = USER_AGENT_ANDROID_VR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_VR,
            clientVersion = CLIENT_VERSION_ANDROID_VR,
            clientName = "ANDROID_VR",
            friendlyName = if (useAV1())
                "Android VR AV1"
            else
                "Android VR"
        ),
        ANDROID_VR_NO_AUTH(
            id = 28,
            deviceMake = DEVICE_MAKE_ANDROID_VR,
            deviceModel = DEVICE_MODEL_ANDROID_VR,
            osVersion = OS_VERSION_ANDROID_VR,
            userAgent = USER_AGENT_ANDROID_VR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_VR,
            clientVersion = CLIENT_VERSION_ANDROID_VR,
            supportsCookies = false,
            clientName = "ANDROID_VR",
            friendlyName = if (useAV1())
                "Android VR AV1"
            else
                "Android VR"
        ),
        ANDROID_CREATOR(
            id = 14,
            deviceMake = DEVICE_MAKE_ANDROID_CREATOR,
            deviceModel = DEVICE_MODEL_ANDROID_CREATOR,
            osVersion = OS_VERSION_ANDROID_CREATOR,
            userAgent = USER_AGENT_ANDROID_CREATOR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_CREATOR,
            clientVersion = CLIENT_VERSION_ANDROID_CREATOR,
            requireAuth = true,
            clientName = "ANDROID_CREATOR",
            friendlyName = "Android Studio"
        ),

        // Unreleased.
        VISIONOS(
            id = 101,
            deviceMake = DEVICE_MAKE_VISIONOS,
            deviceModel = DEVICE_MODEL_VISIONOS,
            osName = OS_NAME_VISIONOS,
            osVersion = OS_VERSION_VISIONS,
            userAgent = USER_AGENT_VISIONOS,
            clientVersion = CLIENT_VERSION_VISIONOS,
            clientPlatform = CLIENT_PLATFORM_DESKTOP,
            supportsCookies = false,
            clientName = "VISIONOS",
            friendlyName = "visionOS"
        ),
        TV(
            id = 7,
            clientVersion = CLIENT_VERSION_TVHTML5,
            clientPlatform = CLIENT_PLATFORM_TV,
            userAgent = USER_AGENT_TVHTML5,
            requireJS = true,
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            supportsMultiAudioTracks = true,
            clientName = "TVHTML5",
            friendlyName = "TV"
        ),
        TV_LEGACY(
            id = 7,
            clientVersion = CLIENT_VERSION_TVHTML5_LEGACY,
            clientPlatform = CLIENT_PLATFORM_DESKTOP,
            userAgent = USER_AGENT_TVHTML5_LEGACY,
            requireJS = true,
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            supportsMultiAudioTracks = true,
            clientName = "TVHTML5",
            friendlyName = "TV Legacy"
        ),
        TV_SIMPLY(
            id = 75,
            clientVersion = CLIENT_VERSION_TVHTML5_SIMPLY,
            clientPlatform = CLIENT_PLATFORM_GAME_CONSOLE,
            userAgent = USER_AGENT_TVHTML5_SIMPLY,
            requireJS = true,
            supportsMultiAudioTracks = true,
            clientName = "TVHTML5_SIMPLY",
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            friendlyName = "TV Simply"
        ),

        // Unused client.
        TV_EMBEDDED(
            id = 85,
            clientVersion = CLIENT_VERSION_TVHTML5_EMBEDDED,
            clientPlatform = CLIENT_PLATFORM_TV,
            clientScreen = CLIENT_SCREEN_EMBED,
            userAgent = USER_AGENT_TVHTML5,
            requireJS = true,
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            supportsMultiAudioTracks = true,
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            friendlyName = "TV Embedded"
        ),

        // Unused client.
        MWEB(
            id = 2,
            clientVersion = CLIENT_VERSION_MWEB,
            userAgent = USER_AGENT_MWEB,
            requireJS = true,
            requirePoToken = true,
            // Android YouTube app does not support 'Cookie'?.
            supportsCookies = false,
            refererFormat = CLIENT_REFERER_FORMAT_MWEB,
            clientName = "MWEB",
            friendlyName = "Mobile Web"
        ),

        // Unused client.
        WEB_LEGACY(
            id = 1,
            clientVersion = CLIENT_VERSION_WEB_LEGACY,
            clientPlatform = CLIENT_PLATFORM_DESKTOP,
            userAgent = USER_AGENT_WEB_LEGACY,
            requireJS = true,
            requirePoToken = true,
            // Android YouTube app does not support 'Cookie'?.
            supportsCookies = false,
            refererFormat = CLIENT_REFERER_FORMAT_WEB,
            clientName = "WEB",
            friendlyName = "Web"
        );

        companion object {
            val CLIENT_ORDER_TO_USE_YOUTUBE: Array<ClientType> = arrayOf(
                ANDROID_NO_SDK,
                VISIONOS,
                ANDROID_VR,
                ANDROID_CREATOR,
            )
            val CLIENT_ORDER_TO_USE_JS_YOUTUBE: Array<ClientType> = arrayOf(
                ANDROID_NO_SDK,
                VISIONOS,
                ANDROID_VR,
                ANDROID_CREATOR,
                TV_SIMPLY,
                TV_LEGACY,
                TV,
            )
            val CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC: Array<ClientType> = arrayOf(
                ANDROID_MUSIC_NO_SDK,
                ANDROID_NO_SDK,
                VISIONOS,
                ANDROID_VR,
            )
            val CLIENT_ORDER_TO_USE_JS_YOUTUBE_MUSIC: Array<ClientType> = arrayOf(
                ANDROID_MUSIC_NO_SDK,
                ANDROID_NO_SDK,
                VISIONOS,
                ANDROID_VR,
                TV_SIMPLY,
                TV_LEGACY,
                TV,
            )
        }
    }
}
