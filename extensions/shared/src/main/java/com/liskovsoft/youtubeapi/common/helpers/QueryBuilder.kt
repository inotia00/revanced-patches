package com.liskovsoft.youtubeapi.common.helpers

import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils
import app.revanced.extension.shared.utils.Utils
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class PostDataType { Player, Browse }

// Use protobuf to bypass geo blocking
private const val GEO_PARAMS: String = "CgIQBg%3D%3D"

@Suppress("DEPRECATION", "unused")
internal class QueryBuilder(private val client: AppClient) {
    private var type: PostDataType = PostDataType.Player
    private var acceptLanguage: String? = null
    private var acceptRegion: String? = null
    private var utcOffsetMinutes: Int? = null
    private var visitorData: String? = null
    private var cpn: String? = null
    private var browseId: String? = null
    private var continuationId: String? = null
    private var videoId: String? = null
    private var playlistId: String? = null
    private var playlistIndex: Int? = null
    private var clickTrackingParams: String? = null
    private var params: String? = null
    private var poToken: String? = null
    private var signatureTimestamp: Int? = null
    private val isWebEmbedded: Boolean = client == AppClient.WEB_EMBED
    private var isGeoFixEnabled: Boolean = false

    fun setType(type: PostDataType) = apply { this.type = type }
    fun setLanguage(lang: String?) = apply { acceptLanguage = lang }
    fun setCountry(country: String?) = apply { acceptRegion = country }
    fun setUtcOffsetMinutes(offset: Int?) = apply { utcOffsetMinutes = offset }
    fun setBrowseId(browseId: String?) = apply { this.browseId = browseId }
    fun setContinuationId(continuationId: String?) = apply { this.continuationId = continuationId }
    fun setVideoId(videoId: String?) = apply { this.videoId = videoId }
    fun setPlaylistId(playlistId: String?) = apply { this.playlistId = playlistId }
    fun setPlaylistIndex(playlistIndex: Int?) = apply { this.playlistIndex = playlistIndex }
    fun setPoToken(poToken: String?) = apply { this.poToken = poToken }
    fun setClientPlaybackNonce(cpn: String?) = apply { this.cpn = cpn }
    fun setSignatureTimestamp(timestamp: Int?) = apply { signatureTimestamp = timestamp }
    fun setClickTrackingParams(params: String?) = apply { clickTrackingParams = params }
    fun setParams(params: String?) = apply { this.params = params }
    fun setVisitorData(visitorData: String?) = apply { this.visitorData = visitorData }
    fun enableGeoFix(enableGeoFix: Boolean) = apply { isGeoFixEnabled = enableGeoFix }

    fun build(): String {
        if (acceptLanguage == null || acceptRegion == null || utcOffsetMinutes == null) {
            val locale: Locale = Utils.getContext().resources.configuration.locale
            val country = locale.country
            val language = locale.toLanguageTag()
            val tz: TimeZone = TimeZone.getDefault()
            val now = Date()
            val utcOffsetMinute = tz.getOffset(now.time) / 1_000 / 60

            acceptLanguage = language
            acceptRegion = country
            utcOffsetMinutes = utcOffsetMinute
        }

        if (playerDataCheck() || browseDataCheck()) {
            if (visitorData == null)
                visitorData = ThrottlingParameterUtils.getVisitorId()
        }

        if (playerDataCheck()) {
            // if (cpn == null)
            //     cpn = appService.clientPlaybackNonce // get it somewhere else?

            if (signatureTimestamp == null)
                signatureTimestamp =
                    ThrottlingParameterUtils.getSignatureTimestamp() // get it somewhere else?
        }

        val json = """
             {
                "context": {
                     ${createClientChunk()}
                     ${createClickTrackingChunk()}
                     ${createUserChunk()}
                     ${createWebEmbeddedChunk()}
                },
                ${createTimestampChunk()}
                ${createPotChunk()}
                ${createVideoDataChunk()}
                ${createBrowseDataChunk()}
             }
        """

        // Remove all indentations
        val result = buildString {
            json.lineSequence().forEach { append(it.trim()) }
        }

        return result
    }

    private fun createClientChunk(): String {
        val clientVars = """
            "clientName": "${client.clientName}",
            "clientVersion": "${client.clientVersion}",
            "clientScreen": "${client.clientScreen}",
            "userAgent": "${client.userAgent}",
        """
        val browserVars = if (client.browserName != null && client.browserVersion != null)
            """
                "browserName": "${client.browserName}",
                "browserVersion": "${client.browserVersion}",
            """
        else ""
        val postVars = client.postData ?: ""
        val postBrowseVars = if (requireNotNull(type) == PostDataType.Browse)
            client.postDataBrowse ?: ""
        else ""
        val regionVars = """
            "acceptLanguage": "${requireNotNull(acceptLanguage)}",
            "acceptRegion": "${requireNotNull(acceptRegion)}",
            "utcOffsetMinutes": "${requireNotNull(utcOffsetMinutes)}",
        """
        val visitorVar = visitorData?.let { """ "visitorData": "$visitorData" """ } ?: ""
        return """
             "client": {
                $clientVars
                $browserVars
                $postVars
                $postBrowseVars
                $regionVars
                $visitorVar
             },
        """
    }

    private fun createClickTrackingChunk(): String {
        return clickTrackingParams?.let {
            """
                "clickTracking": {
                    "clickTrackingParams": "$it"
                },
            """
        } ?: ""
    }

    private fun createWebEmbeddedChunk(): String {
        return if (isWebEmbedded)
            """
                "thirdParty": {
                    "embedUrl": "https://www.youtube.com/embed/${requireNotNull(videoId)}"
                },
            """
        else ""
    }

    private fun createUserChunk(): String {
        return """
           "user":{
                "enableSafetyMode": false,
                "lockedSafetyMode":false
           }, 
        """
    }

    private fun createPotChunk(): String {
        return poToken?.let {
            """
               "serviceIntegrityDimensions": {
                    "poToken": "$it"
               }, 
            """
        } ?: ""
    }

    private fun createVideoDataChunk(): String {
        val data = """
                    "racyCheckOk": true,
                    "contentCheckOk": true,
                    ${createVideoIdChunk()}
                    ${createCPNChunk()}
                """
        return if (client.isReelPlayer)
            """
                "playerRequest": {
                    $data
                },
            """
        else
            """
                $data
            """
    }

    private fun createBrowseDataChunk(): String {
        return """
                    ${createBrowseIdChunk()}
                    ${createContinuationIdChunk()}
                    ${createPlaylistIdChunk()}
                    ${createParamsChunk()}
                """
    }

    private fun createVideoIdChunk(): String {
        return videoId?.let {
            """
                "videoId": "$it",
            """
        } ?: ""
    }

    private fun createBrowseIdChunk(): String {
        return browseId?.let {
            """
                "browseId": "$it",
            """
        } ?: ""
    }

    private fun createContinuationIdChunk(): String {
        return continuationId?.let {
            """
                "continuation": "$it",
            """
        } ?: ""
    }

    private fun createPlaylistIdChunk(): String {
        // Note, that negative playlistIndex values produce error
        return playlistId?.let {
            """
                "playlistId": "$it",
                "playlistIndex": "${playlistIndex?.coerceAtLeast(0) ?: 0}",
            """
        } ?: ""
    }

    private fun createCPNChunk(): String {
        return cpn?.let {
            """
                "cpn": "$it",
            """
        } ?: ""
    }

    private fun createParamsChunk(): String {
        val params = if (isGeoFixEnabled) GEO_PARAMS else params ?: client.params
        return params?.let {
            """
                "params": "$it",
            """
        } ?: ""
    }

    private fun createTimestampChunk(): String {
        // adPlaybackContext https://github.com/yt-dlp/yt-dlp/commit/ff6f94041aeee19c5559e1c1cd693960a1c1dd14
        // isInlinePlaybackNoAd https://iter.ca/post/yt-adblock/
        //     "playbackContext": {
        //        "adPlaybackContext": {
        //            "pyv": true,
        //            "adType": "AD_TYPE_INSTREAM"
        //        },
        //        "contentPlaybackContext": {
        //            "isInlinePlaybackNoAd": true,
        //        }
        //    },
        return signatureTimestamp?.let {
            """
                "playbackContext": {
                    "contentPlaybackContext": {
                        "html5Preference": "HTML5_PREF_WANTS",
                        "lactMilliseconds": 60000,
                        "isInlinePlaybackNoAd": true,
                        "signatureTimestamp": $it
                    }
                },
            """
        } ?: ""
    }

    private fun playerDataCheck() = videoId != null && type == PostDataType.Player
    private fun browseDataCheck() = type == PostDataType.Browse
}