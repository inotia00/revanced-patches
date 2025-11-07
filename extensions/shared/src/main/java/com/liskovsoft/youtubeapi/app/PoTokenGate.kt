package com.liskovsoft.youtubeapi.app

import com.liskovsoft.youtubeapi.app.potokennp2.PoTokenProviderImpl
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenResult

private enum class PoTokenType {
    /**
     * A poToken generated from videoId.
     *
     * Used in player requests.
     */
    CONTENT,

    /**
     * A generic poToken.
     *
     * Used in SABR requests.
     */
    SESSION
}

internal object PoTokenGate {
    private var mWebPoToken: PoTokenResult? = null
    private var mCacheResetTimeMs: Long = -1

    @JvmStatic
    fun getWebContentPoToken(videoId: String): String? {
        if (!PoTokenProviderImpl.isWebPotSupported) return null

        if (mWebPoToken?.videoId == videoId && !PoTokenProviderImpl.isWebPotExpired) {
            return mWebPoToken?.playerRequestPoToken
        }

        mWebPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        return mWebPoToken?.playerRequestPoToken
    }

    @JvmStatic
    fun getWebSessionPoToken(videoId: String): String? {
        if (!PoTokenProviderImpl.isWebPotSupported) return null

        if (mWebPoToken?.videoId == videoId) {
            val streamingDataPoToken = mWebPoToken!!.streamingDataPoToken
            if (streamingDataPoToken != null) {
                return streamingDataPoToken
            }
        }

        mWebPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        if (mWebPoToken != null) {
            val streamingDataPoToken = mWebPoToken!!.streamingDataPoToken
            if (streamingDataPoToken != null) {
                return streamingDataPoToken
            }
        }

        return null
    }

    @JvmStatic
    fun updatePoToken() {
        if (PoTokenProviderImpl.isWebPotSupported) {
            //mNpPoToken = null // only refresh
            mWebPoToken = PoTokenProviderImpl.getWebClientPoToken("") // refresh and preload
        }
    }

    @JvmStatic
    fun getVisitorData(videoId: String): String? {
        if (!PoTokenProviderImpl.isWebPotSupported) return null

        if (mWebPoToken?.videoId == videoId && !PoTokenProviderImpl.isWebPotExpired) {
            return mWebPoToken?.visitorData
        }

        mWebPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        return mWebPoToken?.visitorData
    }

    @JvmStatic
    fun resetCache(): Boolean {
        if (System.currentTimeMillis() < mCacheResetTimeMs) {
            return false
        }

        if (PoTokenProviderImpl.isWebPotSupported) {
            mWebPoToken = null
            PoTokenProviderImpl.resetCache()
        }

        mCacheResetTimeMs = System.currentTimeMillis() + 60_000

        return true
    }
}