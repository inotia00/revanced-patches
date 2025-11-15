package app.revanced.extension.shared.innertube.requests

import app.revanced.extension.shared.innertube.client.YouTubeClient
import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.requests.Route.CompiledRoute
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.StringRef.str
import app.revanced.extension.shared.utils.Utils
import com.liskovsoft.youtubeapi.app.PoTokenGate
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("deprecation")
object InnerTubeRequestBody {
    private const val YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/"

    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val PAGE_ID_HEADER = "X-Goog-PageId"
    private const val VISITOR_ID_HEADER: String = "X-Goog-Visitor-Id"
    private val REQUEST_HEADER_KEYS = setOf(
        AUTHORIZATION_HEADER,  // Available only to logged-in users.
        PAGE_ID_HEADER,
        VISITOR_ID_HEADER,
    )

    /**
     * TCP connection and HTTP read timeout
     */
    private const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000 // 10 Seconds.

    private val LOCALE: Locale by lazy {
        Utils.getContext().resources.configuration.locale
    }
    private val LOCALE_COUNTRY: String by lazy {
        LOCALE.country
    }
    private val LOCALE_LANGUAGE: String by lazy {
        LOCALE.language
    }
    private val TIME_ZONE: TimeZone = TimeZone.getDefault()
    private val TIME_ZONE_ID: String = TIME_ZONE.id
    private val UTC_OFFSET_MINUTES: Int = TIME_ZONE.getOffset(Date().time) / 60000

    @JvmStatic
    fun createApplicationRequestBody(
        clientType: YouTubeClient.ClientType,
        videoId: String,
        playlistId: String? = null,
        language: String = LOCALE_LANGUAGE,
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("deviceMake", clientType.deviceMake)
            client.put("deviceModel", clientType.deviceModel)
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientType.clientVersion)
            client.put("osName", clientType.osName)
            client.put("osVersion", clientType.osVersion)
            if (clientType.androidSdkVersion != null) {
                client.put("androidSdkVersion", clientType.androidSdkVersion)
            }
            client.put("hl", language)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", "$UTC_OFFSET_MINUTES")

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("racyCheckOk", true)
            innerTubeBody.put("videoId", videoId)

            if (playlistId != null) {
                innerTubeBody.put("playlistId", playlistId)
            }
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create application innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun createJSRequestBody(
        clientType: YouTubeClient.ClientType,
        videoId: String,
        cpn: String = "",
        isGVS: Boolean = false,
        isInlinePlayback: Boolean = false,
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", ThrottlingParameterUtils.getClientVersion(clientType))
            client.put("platform", clientType.clientPlatform)
            client.put("clientScreen", clientType.clientScreen)
            client.put("hl", LOCALE_LANGUAGE)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", UTC_OFFSET_MINUTES.toString())

            if (clientType.name.startsWith("TV")) {
                val configInfo = JSONObject()
                configInfo.put("appInstallData", "")
                client.put("configInfo", configInfo)
            }

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("racyCheckOk", true)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("videoId", videoId)
            if (cpn.isNotEmpty()) {
                innerTubeBody.put("cpn", cpn)
            }

            val user = JSONObject()
            user.put("lockedSafetyMode", false)
            context.put("user", user)

            if (isGVS) {
                val contentPlaybackContext = JSONObject()
                val requirePoToken = clientType.requirePoToken
                if (clientType.refererFormat != null) {
                    contentPlaybackContext.put(
                        "referer",
                        String.format(clientType.refererFormat, videoId)
                    )
                }
                contentPlaybackContext.put("html5Preference", "HTML5_PREF_WANTS")
                if (isInlinePlayback) {
                    // https://iter.ca/post/yt-adblock/
                    contentPlaybackContext.put("isInlinePlaybackNoAd", true)
                }
                val signatureTimestamp =
                    ThrottlingParameterUtils.getSignatureTimestamp()
                if (signatureTimestamp != null) {
                    contentPlaybackContext.put("signatureTimestamp", signatureTimestamp)
                }

                val devicePlaybackCapabilities = JSONObject()
                devicePlaybackCapabilities.put("supportsVp9Encoding", true)
                devicePlaybackCapabilities.put("supportXhr", false)

                val playbackContext = JSONObject()
                playbackContext.put("contentPlaybackContext", contentPlaybackContext)
                playbackContext.put("devicePlaybackCapabilities", devicePlaybackCapabilities)

                innerTubeBody.put("playbackContext", playbackContext)

                if (requirePoToken) {
                    val playerRequestPoToken = PoTokenGate.getWebContentPoToken(videoId)
                    if (playerRequestPoToken != null) {
                        val serviceIntegrityDimensions = JSONObject()
                        serviceIntegrityDimensions.put("poToken", playerRequestPoToken)
                        innerTubeBody.put("serviceIntegrityDimensions", serviceIntegrityDimensions)
                    }
                }
            }
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create js innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun androidInnerTubeBody(
        clientType: YouTubeClient.ClientType = YouTubeClient.ClientType.ANDROID
    ): JSONObject {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("deviceMake", clientType.deviceMake)
            client.put("deviceModel", clientType.deviceModel)
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientType.clientVersion)
            client.put("osName", clientType.osName)
            client.put("osVersion", clientType.osVersion)
            client.put("androidSdkVersion", clientType.androidSdkVersion)
            client.put("hl", LOCALE_LANGUAGE)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", UTC_OFFSET_MINUTES.toString())

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("racyCheckOk", true)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create android innerTubeBody" }, e)
        }

        return innerTubeBody
    }

    @JvmStatic
    fun createPlaylistRequestBody(
        videoId: String,
    ): ByteArray {
        val innerTubeBody = androidInnerTubeBody()

        try {
            innerTubeBody.put("params", "CAQ%3D")
            // TODO: Implement an AlertDialog that allows changing the title of the playlist.
            innerTubeBody.put("title", str("revanced_queue_manager_queue"))

            val videoIds = JSONArray()
            videoIds.put(0, videoId)
            innerTubeBody.put("videoIds", videoIds)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create create/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun deletePlaylistRequestBody(
        playlistId: String,
    ): ByteArray {
        val innerTubeBody = androidInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create delete/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun editPlaylistRequestBody(
        videoId: String,
        playlistId: String,
        setVideoId: String?,
    ): ByteArray {
        val innerTubeBody = androidInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)

            val actionsObject = JSONObject()
            if (setVideoId != null && setVideoId.isNotEmpty()) {
                actionsObject.put("action", "ACTION_REMOVE_VIDEO")
                actionsObject.put("setVideoId", setVideoId)
            } else {
                actionsObject.put("action", "ACTION_ADD_VIDEO")
                actionsObject.put("addedVideoId", videoId)
            }

            val actionsArray = JSONArray()
            actionsArray.put(0, actionsObject)
            innerTubeBody.put("actions", actionsArray)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create edit/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getPlaylistsRequestBody(
        playlistId: String,
    ): ByteArray {
        val innerTubeBody = androidInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)
            innerTubeBody.put("excludeWatchLater", false)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create get/playlists innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun savePlaylistRequestBody(
        playlistId: String,
        libraryId: String,
    ): ByteArray {
        val innerTubeBody = androidInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)

            val actionsObject = JSONObject()
            actionsObject.put("action", "ACTION_ADD_PLAYLIST")
            actionsObject.put("addedFullListId", libraryId)

            val actionsArray = JSONArray()
            actionsArray.put(0, actionsObject)
            innerTubeBody.put("actions", actionsArray)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create save/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getInnerTubeResponseConnectionFromRoute(
        route: CompiledRoute,
        clientType: YouTubeClient.ClientType,
        requestHeader: Map<String, String>? = null,
        connectTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
        readTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
    ) = getInnerTubeResponseConnectionFromRoute(
        route = route,
        userAgent = clientType.userAgent,
        clientId = clientType.id.toString(),
        clientVersion = clientType.clientVersion,
        supportsCookies = clientType.supportsCookies,
        requestHeader = requestHeader,
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
    )

    @Throws(IOException::class)
    fun getInnerTubeResponseConnectionFromRoute(
        route: CompiledRoute,
        userAgent: String,
        clientId: String,
        clientVersion: String,
        supportsCookies: Boolean = true,
        requestHeader: Map<String, String>? = null,
        connectTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
        readTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
    ): HttpURLConnection {
        val connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route)

        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("X-YouTube-Client-Name", clientId)
        connection.setRequestProperty("X-YouTube-Client-Version", clientVersion)
        connection.setRequestProperty("X-GOOG-API-FORMAT-VERSION", "2")

        connection.useCaches = false
        connection.doOutput = true

        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout

        if (requestHeader != null) {
            for (key in REQUEST_HEADER_KEYS) {
                if (!supportsCookies && StringUtils.equalsAny(
                        key,
                        AUTHORIZATION_HEADER,
                        PAGE_ID_HEADER,
                    )
                ) {
                    continue
                }
                val value = requestHeader[key]
                if (value != null) {
                    connection.setRequestProperty(key, value)
                }
            }
        }

        return connection
    }

}