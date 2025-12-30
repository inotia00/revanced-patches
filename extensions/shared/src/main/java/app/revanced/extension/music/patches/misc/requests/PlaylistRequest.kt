package app.revanced.extension.music.patches.misc.requests

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.innertube.client.YouTubeClient
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.revanced.extension.shared.innertube.requests.InnerTubeRoutes.GET_PLAYLIST_PAGE
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.settings.AppLanguage
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import app.revanced.extension.shared.utils.Utils.isSDKAbove
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PlaylistRequest private constructor(
    private val videoId: String,
    private val playlistId: String,
    private val playlistIndex: Int,
) {
    /**
     * Time this instance and the fetch future was created.
     */
    private val timeFetched = System.currentTimeMillis()
    private val future: Future<String> = Utils.submitOnBackgroundThread {
        fetch(
            videoId,
            playlistId,
            playlistIndex,
        )
    }

    fun isExpired(now: Long): Boolean {
        val timeSinceCreation = now - timeFetched
        if (timeSinceCreation > CACHE_RETENTION_TIME_MILLISECONDS) {
            return true
        }

        // Only expired if the fetch failed (API null response).
        return (fetchCompleted() && songId.isEmpty())
    }

    /**
     * @return if the fetch call has completed.
     */
    fun fetchCompleted(): Boolean {
        return future.isDone
    }

    val songId: String
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getSongId timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getSongId interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getSongId failure" },
                    ex
                )
            }

            return ""
        }

    companion object {
        /**
         * How long to keep fetches until they are expired.
         */
        private const val CACHE_RETENTION_TIME_MILLISECONDS = 60 * 1000L // 1 Minute

        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 10 * 1000L // 10 seconds

        @GuardedBy("itself")
        private val cache: MutableMap<String, PlaylistRequest> = HashMap()

        @JvmStatic
        fun fetchRequestIfNeeded(
            videoId: String,
            playlistId: String,
            playlistIndex: Int,
        ) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                val now = System.currentTimeMillis()
                if (isSDKAbove(24)) {
                    cache.values.removeIf { request ->
                        val expired = request.isExpired(now)
                        if (expired) Logger.printDebug { "Removing expired song id: " + request.videoId }
                        expired
                    }
                } else {
                    val itr = cache.entries.iterator()
                    while (itr.hasNext()) {
                        val request = itr.next().value
                        if (request.isExpired(now)) {
                            Logger.printDebug { "Removing expired song id: " + request.videoId }
                            itr.remove()
                        }
                    }
                }

                if (!cache.containsKey(videoId)) {
                    cache[videoId] = PlaylistRequest(
                        videoId,
                        playlistId,
                        playlistIndex,
                    )
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): PlaylistRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(
            videoId: String,
            playlistId: String,
        ): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            val clientType = YouTubeClient.ClientType.ANDROID_VR_NO_AUTH
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playlist request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_PLAYLIST_PAGE,
                    clientType
                )

                /**
                 * For some reason, the tracks in Top Songs have the playlistId of the album:
                 * [ReVanced_Extended#2835](https://github.com/inotia00/ReVanced_Extended/issues/2835)
                 *
                 * We can work around this issue by checking the playlist title in the response.
                 * Tracks played from an album have a playlist title that starts with 'Album',
                 * Tracks played from Top Songs have a playlist title that starts with 'Song'.
                 *
                 * By default, the playlist title follows the app language,
                 * So we can work around this by setting the language to English when sending the request.
                 */
                val requestBody =
                    createApplicationRequestBody(
                        clientType = clientType,
                        videoId = videoId,
                        playlistId = playlistId,
                        language = AppLanguage.EN.language,
                    )

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                if (responseCode == 200) return Requester.parseJSONObject(connection)

                handleConnectionError(
                    (clientTypeName + " not available with response code: "
                            + responseCode + " message: " + connection.responseMessage),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "sendRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseResponse(playlistJson: JSONObject, playlistIndex: Int): String {
            try {
                val singleColumnWatchNextResultsJsonObject: JSONObject =
                    playlistJson
                        .getJSONObject("contents")
                        .getJSONObject("singleColumnWatchNextResults")

                if (singleColumnWatchNextResultsJsonObject.has("playlist")) {
                    val playlistJsonObject: JSONObject? =
                        singleColumnWatchNextResultsJsonObject
                            .getJSONObject("playlist")
                            .getJSONObject("playlist")

                    val playlistTitle = playlistJsonObject
                        ?.getString("title") + ""

                    if (playlistTitle.startsWith("Album")) {
                        val currentStreamJsonObject = playlistJsonObject
                            ?.getJSONArray("contents")
                            ?.get(playlistIndex)

                        if (currentStreamJsonObject is JSONObject) {
                            val watchEndpointJsonObject: JSONObject? =
                                currentStreamJsonObject
                                    .getJSONObject("playlistPanelVideoRenderer")
                                    .getJSONObject("navigationEndpoint")
                                    .getJSONObject("watchEndpoint")

                            return watchEndpointJsonObject?.getString("videoId") + ""
                        }
                    }
                }
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing response data for response: $playlistJson" },
                    e
                )
            }

            return ""
        }

        private fun fetch(
            videoId: String,
            playlistId: String,
            playlistIndex: Int,
        ): String {
            val playlistJson = sendRequest(
                videoId,
                playlistId,
            )
            if (playlistJson != null) {
                return parseResponse(playlistJson, playlistIndex)
            }

            return ""
        }
    }
}
