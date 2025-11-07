package com.liskovsoft.youtubeapi.app.playerdata

import app.revanced.extension.shared.utils.Logger
import com.liskovsoft.youtubeapi.app.nsigsolver.impl.V8ChallengeProvider
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.ChallengeInput
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeRequest
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeType

internal class PlayerDataExtractor(playerJS: String, playerJSIdentifier: String) {
    private var nSigTmp: Pair<String, String?>? = null
    private var initialized: Boolean = false

    init {
        if (!initialized) {
            initialized = true

            V8ChallengeProvider.setPlayerJS(playerJS, playerJSIdentifier)

            checkAllData()
        }
    }

    fun extractNSig(nParam: String): String? {
        if (nSigTmp?.first == nParam) return nSigTmp?.second

        val nSig = extractNSigReal(nParam)

        nSigTmp = Pair(nParam, nSig)

        return nSig
    }

    fun extractSig(sParam: String): String? {
        val result = V8ChallengeProvider.bulkSolve(
            listOfNotNull(JsChallengeRequest(JsChallengeType.SIG, ChallengeInput(sParam)))
        )

        return result.toList().firstOrNull()?.response?.output?.results?.get(sParam)
    }

    private fun extractNSigReal(nParam: String): String? {
        val result = V8ChallengeProvider.bulkSolve(
            listOfNotNull(JsChallengeRequest(JsChallengeType.N, ChallengeInput(nParam)))
        )

        return result.toList().firstOrNull()?.response?.output?.results?.get(nParam)
    }

    private fun checkAllData() {
        val param = "5cNpZqIJ7ixNqU68Y7S"

        try {
            V8ChallengeProvider.bulkSolve(
                listOf(
                    JsChallengeRequest(JsChallengeType.N, ChallengeInput(param)),
                    JsChallengeRequest(JsChallengeType.SIG, ChallengeInput(param)),
                )
            )
        } catch (e: Exception) {
            Logger.printException({ "checkAllData failed" }, e)
        }
    }
}