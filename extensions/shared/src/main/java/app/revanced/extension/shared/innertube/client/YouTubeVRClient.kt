package app.revanced.extension.shared.innertube.client

import android.os.Build
import java.util.Locale
import kotlin.Deprecated

@Suppress("SpellCheckingInspection", "unused")
object YouTubeVRClient {
    private const val PACKAGE_NAME_OCULUS =
        "com.google.android.apps.youtube.vr.oculus"
    private const val PACKAGE_NAME_PICO =
        "com.google.android.apps.youtube.vr.pico"
    private const val PACKAGE_NAME_XR =
        "com.google.android.apps.youtube.xr"

    private const val CLIENT_VERSION_OCULUS_1_43 = "1.43.32" // Last version of minSdkVersion 24.
    private const val CLIENT_VERSION_OCULUS_1_65 = "1.65.10"
    private const val CLIENT_VERSION_PICO = "1.64.34"
    private const val CLIENT_VERSION_XR = "1.69.27"

    private const val DEVICE_MODEL_MOOHAN = "SM-I610"

    private const val DEVICE_MODEL_PICO_4 = "A8110"
    private const val DEVICE_MODEL_PICO_4_ULTRA = "A9210"

    private const val DEVICE_MODEL_QUEST = "Quest"
    private const val DEVICE_MODEL_QUEST_2 = "Quest 2"
    private const val DEVICE_MODEL_QUEST_3 = "Quest 3"
    private const val DEVICE_MODEL_QUEST_3S = "Quest 3S"
    private const val DEVICE_MODEL_QUEST_PRO = "Quest Pro"

    private const val OS_VERSION_HORIZON_OS_25 = "7.1.1"
    private const val OS_VERSION_HORIZON_OS_76 = "14"

    private const val OS_VERSION_ONE_UI_XR = "14"

    private const val OS_VERSION_MOOHAN = OS_VERSION_ONE_UI_XR

    private const val OS_VERSION_PICO_4 = "10"
    private const val OS_VERSION_PICO_4_ULTRA = "14"

    private const val OS_VERSION_QUEST = OS_VERSION_HORIZON_OS_25
    private const val OS_VERSION_QUEST_2 = OS_VERSION_HORIZON_OS_76
    private const val OS_VERSION_QUEST_3 = OS_VERSION_HORIZON_OS_76
    private const val OS_VERSION_QUEST_3S = OS_VERSION_HORIZON_OS_76
    private const val OS_VERSION_QUEST_PRO = OS_VERSION_HORIZON_OS_76

    private const val BUILD_ID_HORIZON_OS_25 = "NGI77B"
    private const val BUILD_ID_HORIZON_OS_76 = "UP1A.231005.007.A1"

    private const val BUILD_ID_ONE_UI_XR = "UML1.250710.002.A1"

    private const val BUILD_ID_MOOHAN = BUILD_ID_ONE_UI_XR

    // Unlike other Android devices, the Pico 4 is the only device that uses the PicoOS version name as the 'ro.vendor.build.id' value.
    private const val BUILD_ID_PICO_4 = "5.13.3"
    private const val BUILD_ID_PICO_4_ULTRA = "UKQ1.240321.001"

    private const val BUILD_ID_QUEST = BUILD_ID_HORIZON_OS_25
    private const val BUILD_ID_QUEST_2 = BUILD_ID_HORIZON_OS_76
    private const val BUILD_ID_QUEST_3 = BUILD_ID_HORIZON_OS_76
    private const val BUILD_ID_QUEST_3S = BUILD_ID_HORIZON_OS_76
    private const val BUILD_ID_QUEST_PRO = BUILD_ID_HORIZON_OS_76

    private val USER_AGENT_MOOHAN = xrUserAgent(
        osVersion = OS_VERSION_MOOHAN,
        deviceModel = DEVICE_MODEL_MOOHAN,
        buildId = BUILD_ID_MOOHAN
    )

    private val USER_AGENT_PICO_4 = picoUserAgent(
        osVersion = OS_VERSION_PICO_4,
        deviceModel = DEVICE_MODEL_PICO_4,
        buildId = BUILD_ID_PICO_4
    )

    private val USER_AGENT_PICO_4_ULTRA = picoUserAgent(
        osVersion = OS_VERSION_PICO_4_ULTRA,
        deviceModel = DEVICE_MODEL_PICO_4_ULTRA,
        buildId = BUILD_ID_PICO_4_ULTRA
    )

    private val USER_AGENT_QUEST = oculusUserAgent(
        clientVersion = CLIENT_VERSION_OCULUS_1_43,
        osVersion = OS_VERSION_QUEST,
        deviceModel = DEVICE_MODEL_QUEST,
        buildId = BUILD_ID_QUEST
    )

    private val USER_AGENT_QUEST_2 = oculusUserAgent(
        clientVersion = CLIENT_VERSION_OCULUS_1_65,
        osVersion = OS_VERSION_QUEST_2,
        deviceModel = DEVICE_MODEL_QUEST_2,
        buildId = BUILD_ID_QUEST_2
    )

    private val USER_AGENT_QUEST_3 = oculusUserAgent(
        clientVersion = CLIENT_VERSION_OCULUS_1_65,
        osVersion = OS_VERSION_QUEST_3,
        deviceModel = DEVICE_MODEL_QUEST_3,
        buildId = BUILD_ID_QUEST_3
    )

    private val USER_AGENT_QUEST_3S = oculusUserAgent(
        clientVersion = CLIENT_VERSION_OCULUS_1_65,
        osVersion = OS_VERSION_QUEST_3S,
        deviceModel = DEVICE_MODEL_QUEST_3S,
        buildId = BUILD_ID_QUEST_3S
    )

    private val USER_AGENT_QUEST_PRO = oculusUserAgent(
        clientVersion = CLIENT_VERSION_OCULUS_1_65,
        osVersion = OS_VERSION_QUEST_PRO,
        deviceModel = DEVICE_MODEL_QUEST_PRO,
        buildId = BUILD_ID_QUEST_PRO
    )

    private fun oculusUserAgent(
        clientVersion: String,
        osVersion: String,
        deviceModel: String,
        buildId: String,
    ) = androidUserAgent(
        packageName = PACKAGE_NAME_OCULUS,
        clientVersion = clientVersion,
        osVersion = osVersion,
        deviceModel = deviceModel,
        buildId = buildId
    )

    private fun picoUserAgent(
        osVersion: String,
        deviceModel: String,
        buildId: String,
    ) = androidUserAgent(
        packageName = PACKAGE_NAME_PICO,
        clientVersion = CLIENT_VERSION_PICO,
        osVersion = osVersion,
        deviceModel = deviceModel,
        buildId = buildId
    )

    @Suppress("SameParameterValue")
    private fun xrUserAgent(
        osVersion: String,
        deviceModel: String,
        buildId: String,
    ) = androidUserAgent(
        packageName = PACKAGE_NAME_XR,
        clientVersion = CLIENT_VERSION_XR,
        osVersion = osVersion,
        deviceModel = deviceModel,
        buildId = buildId
    )

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

    /**
     * Do not change the order.
     * [ClientType] matches the order of the enum classes in the YouTube VR app.
     */
    enum class ClientType(
        /**
         * Client user-agent.
         */
        val userAgent: String? = null,
    ) {
        /**
         * Legacy devices.
         */
        UNKNOWN,

        /**
         * Oculus Quest (May 21, 2019).
         */
        QUEST1(USER_AGENT_QUEST),

        /**
         * Quest2 (October 13, 2020).
         */
        QUEST2(USER_AGENT_QUEST_2),

        /**
         * Meta Quest Pro (October 25, 2022).
         */
        QUEST_PRO(USER_AGENT_QUEST_PRO),

        /**
         * Galaxy XR (October 22, 2025).
         */
        @Deprecated("This was the client name used before the release of Android XR in 2023-2025. Use ANDROID_XR instead.")
        MOOHAN,

        /**
         * PICO 4 (October 18, 2022).
         * Although it was released before [QUEST_PRO], YouTube VR for Pico was rolled out around December 2023.
         * so it has a later order in the enum class.
         */
        PICO4(USER_AGENT_PICO_4),

        /**
         * Meta Quest 3 (October 10, 2023).
         */
        QUEST3(USER_AGENT_QUEST_3),

        /**
         * Meta Quest 3S (October 15, 2024).
         */
        QUEST3S(USER_AGENT_QUEST_3S),

        /**
         * PICO 4 Ultra (September 2, 2024).
         */
        PICO4_ULTRA(USER_AGENT_PICO_4_ULTRA),

        /**
         * Galaxy XR (October 22, 2025).
         */
        ANDROID_XR(USER_AGENT_MOOHAN);
    }
}