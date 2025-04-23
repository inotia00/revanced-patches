package app.revanced.extension.shared.patches.spoof.requests

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.innertube.client.YouTubeAppClient
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.revanced.extension.shared.innertube.requests.InnerTubeRoutes.GET_STREAMING_DATA
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.StringRef.str
import app.revanced.extension.shared.utils.Utils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Video streaming data.  Fetching is tied to the behavior YT uses,
 * where this class fetches the streams only when YT fetches.
 *
 * Effectively the cache expiration of these fetches is the same as the stock app,
 * since the stock app would not use expired streams and therefor
 * the extension replace stream hook is called only if YT
 * did use its own client streams.
 */
class StreamingDataRequest private constructor(
    videoId: String,
    requestHeader: Map<String, String>,
) {
    private val videoId: String
    private val future: Future<ByteBuffer?>

    init {
        Objects.requireNonNull(requestHeader)
        this.videoId = videoId
        this.future = Utils.submitOnBackgroundThread {
            fetch(
                videoId,
                requestHeader,
            )
        }
    }

    fun fetchCompleted(): Boolean {
        return future.isDone
    }

    val stream: ByteBuffer?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getStream timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getStream interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getStream failure" },
                    ex
                )
            }

            return null
        }

    override fun toString(): String {
        return "StreamingDataRequest{videoId='$videoId'}"
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 10 * 1000

        private val SPOOF_STREAMING_DATA_TYPE: YouTubeAppClient.ClientType =
            BaseSettings.SPOOF_STREAMING_DATA_TYPE.get()
        private val CLIENT_ORDER_TO_USE: Array<YouTubeAppClient.ClientType> =
            YouTubeAppClient.availableClientTypes(SPOOF_STREAMING_DATA_TYPE)
        private val DEFAULT_CLIENT_IS_ANDROID_VR_NO_AUTH: Boolean =
            SPOOF_STREAMING_DATA_TYPE == YouTubeAppClient.ClientType.ANDROID_VR_NO_AUTH

        private var lastSpoofedClientFriendlyName: String? = null

        @GuardedBy("itself")
        val cache: MutableMap<String, StreamingDataRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, StreamingDataRequest>(1) {
                private val CACHE_LIMIT = 10

                override fun removeEldestEntry(eldest: Map.Entry<String, StreamingDataRequest>): Boolean {
                    return size >= CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        val lastSpoofedClientName: String
            get() {
                return if (lastSpoofedClientFriendlyName != null) {
                    lastSpoofedClientFriendlyName!!
                } else {
                    "Unknown"
                }
            }

        @JvmStatic
        fun fetchRequest(
            videoId: String,
            fetchHeaders: Map<String, String>,
        ) {
            // Always fetch, even if there is an existing request for the same video.
            cache[videoId] =
                StreamingDataRequest(
                    videoId,
                    fetchHeaders
                )
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): StreamingDataRequest? {
            return cache[videoId]
        }

        private fun handleConnectionError(
            toastMessage: String,
            ex: Exception?,
            showToast: Boolean = false,
        ) {
            if (showToast) Utils.showToastShort(toastMessage)
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun send(
            clientType: YouTubeAppClient.ClientType,
            videoId: String,
            requestHeader: Map<String, String>,
        ): HttpURLConnection? {
            Objects.requireNonNull(clientType)
            Objects.requireNonNull(videoId)
            Objects.requireNonNull(requestHeader)

            val startTime = System.currentTimeMillis()
            Logger.printDebug { "Fetching video streams for: $videoId using client: $clientType" }

            try {
                val connection =
                    getInnerTubeResponseConnectionFromRoute(
                        GET_STREAMING_DATA,
                        clientType,
                        requestHeader
                    )

                val requestBody = createApplicationRequestBody(
                    clientType = clientType,
                    videoId = videoId,
                    setLocale = DEFAULT_CLIENT_IS_ANDROID_VR_NO_AUTH,
                )

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                if (responseCode == 200) return connection

                // This situation likely means the patches are outdated.
                // Use a toast message that suggests updating.
                handleConnectionError(
                    ("Playback error (App is outdated?) " + clientType + ": "
                            + responseCode + " response: " + connection.responseMessage),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "send failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun fetch(
            videoId: String,
            requestHeader: Map<String, String>,
        ): ByteBuffer? {
            lastSpoofedClientFriendlyName = null

            // Retry with different client if empty response body is received.
            for (clientType in CLIENT_ORDER_TO_USE) {
                if (clientType.requireAuth &&
                    requestHeader[AUTHORIZATION_HEADER] == null
                ) {
                    Logger.printDebug { "Skipped login-required client (incognito mode or not logged in)\nClient: $clientType\nVideo: $videoId" }
                    continue
                }
                send(
                    clientType,
                    videoId,
                    requestHeader,
                )?.let { connection ->
                    try {
                        // gzip encoding doesn't response with content length (-1),
                        // but empty response body does.
                        if (connection.contentLength == 0) {
                            Logger.printDebug { "Received empty response\nClient: $clientType\nVideo: $videoId" }
                        } else {
                            BufferedInputStream(connection.inputStream).use { inputStream ->
                                ByteArrayOutputStream().use { stream ->
                                    val buffer = ByteArray(4096)
                                    var bytesRead: Int
                                    while ((inputStream.read(buffer)
                                            .also { bytesRead = it }) >= 0
                                    ) {
                                        stream.write(buffer, 0, bytesRead)
                                    }
                                    lastSpoofedClientFriendlyName = clientType.friendlyName
                                    return ByteBuffer.wrap(stream.toByteArray())
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        Logger.printException({ "Fetch failed while processing response data" }, ex)
                    }
                }
            }

            handleConnectionError(str("revanced_spoof_streaming_data_failed_forbidden"), null, true)
            handleConnectionError(
                str("revanced_spoof_streaming_data_failed_forbidden_suggestion"),
                null,
                true
            )
            return null
        }
    }
}
