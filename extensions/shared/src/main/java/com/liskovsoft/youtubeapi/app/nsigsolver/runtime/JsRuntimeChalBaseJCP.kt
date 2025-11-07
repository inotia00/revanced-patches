package com.liskovsoft.youtubeapi.app.nsigsolver.runtime

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils
import app.revanced.extension.shared.utils.Logger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.liskovsoft.youtubeapi.app.nsigsolver.common.CachedData
import com.liskovsoft.youtubeapi.app.nsigsolver.common.loadScript
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.ChallengeOutput
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProvider
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProviderError
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProviderRejectedRequest
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProviderResponse
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeRequest
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeResponse
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeType
import java.util.Collections

internal abstract class JsRuntimeChalBaseJCP : JsChallengeProvider() {
    private val tag = JsRuntimeChalBaseJCP::class.simpleName
    val cacheSection = "challenge-solver"
    private var playerJS = ""
    private var playerJSIdentifier = ""

    private val jcpGuideUrl = "https://github.com/yt-dlp/yt-dlp/wiki/YouTube-JS-Challenges"
    private val repository = "yt-dlp/ejs"
    override val supportedTypes = listOf(JsChallengeType.N, JsChallengeType.SIG)
    protected val scriptVersion = "0.0.1"
    internal val libPrefix = "nsigsolver/"

    private val scriptFilenames = mapOf(
        ScriptType.LIB to "${libPrefix}yt.solver.lib.js",
        ScriptType.CORE to "${libPrefix}yt.solver.core.js"
    )

    private val minScriptFilenames = mapOf(
        ScriptType.LIB to "yt.solver.lib.min.js",
        ScriptType.CORE to "yt.solver.core.min.js"
    )

    protected abstract fun runJsRuntime(stdin: String): String

    fun setPlayerJS(
        jsCode: String,
        jsIdentifier: String,
    ) {
        playerJS = jsCode
        playerJSIdentifier = jsIdentifier
    }

    override fun realBulkSolve(requests: List<JsChallengeRequest>): Sequence<JsChallengeProviderResponse> =
        sequence {
            val data = ie.cache.load(cacheSection, "player:$playerJSIdentifier")
            var player = data?.code

            val cached = if (player != null) {
                true
            } else {
                player = playerJS
                false
            }

            val stdin = constructStdin(player, cached, requests)
            val stdout = runJsRuntime(stdin)

            val gson = Gson()
            val output: SolverOutput = try {
                gson.fromJson(stdout, solverOutputType)
            } catch (e: JsonSyntaxException) {
                throw JsChallengeProviderError("Cannot parse solver output", e)
            }

            if (output.type == "error")
                throw JsChallengeProviderError(output.error ?: "Unknown solver output error")

            val preprocessed = output.preprocessed_player
            if (preprocessed != null)
                ie.cache.store(cacheSection, "player:$playerJSIdentifier", CachedData(preprocessed))

            for ((request, responseData) in requests.zip(output.responses)) {
                if (responseData.type == "error") {
                    yield(
                        JsChallengeProviderResponse(
                            request,
                            null,
                            JsChallengeProviderError(
                                responseData.error ?: "Unknown solver output error"
                            )
                        )
                    )
                } else {
                    yield(
                        JsChallengeProviderResponse(
                            request,
                            JsChallengeResponse(request.type, ChallengeOutput(responseData.data))
                        )
                    )
                }
            }
        }

    private fun constructStdin(
        playerJS: String,
        preprocessed: Boolean,
        requests: List<JsChallengeRequest>
    ): String {
        val jsonRequests = requests.map { request ->
            mapOf(
                // TODO: i despise nsig name
                //"type" to if (request.type.value == "n") "nsig" else request.type.value,
                "type" to request.type.value,
                "challenges" to listOf(request.input.challenge)
            )
        }
        val data = if (preprocessed) {
            mapOf(
                "type" to "preprocessed",
                "preprocessed_player" to playerJS,
                "requests" to jsonRequests
            )
        } else {
            mapOf(
                "type" to "player",
                "player" to playerJS,
                "requests" to jsonRequests,
                "output_preprocessed" to true
            )
        }
        val gson = Gson()
        val jsonData = gson.toJson(data)
        return """
        JSON.stringify(jsc($jsonData));
        """
    }

    protected fun constructCommonStdin(): String {
        return """
        ${libScript.code}
        ${coreScript.code}
        "";
        """
    }

    // region: challenge solver script

    private val libScript: Script by lazy {
        getScript(ScriptType.LIB)
    }

    private val coreScript: Script by lazy {
        getScript(ScriptType.CORE)
    }

    private fun getScript(scriptType: ScriptType): Script {
        for ((_, fromSource) in iterScriptSources()) {
            val script = fromSource(scriptType) ?: continue
            if (script.version != scriptVersion)
                Logger.printWarn {
                    "Challenge solver ${scriptType.value} script version ${script.version} " +
                            "is not supported (source: ${script.source.value}, supported version: $scriptVersion)"
                }

            Logger.printDebug {
                "Using challenge solver ${script.type.value} script v${script.version} " +
                        "(source: ${script.source.value}, variant: ${script.variant.value})"
            }
            return script
        }
        throw JsChallengeProviderRejectedRequest("No usable challenge solver ${scriptType.value} script available")
    }

    protected open fun iterScriptSources(): Sequence<Pair<ScriptSource, (scriptType: ScriptType) -> Script?>> =
        sequence {
            yieldAll(
                listOf(
                    Pair(ScriptSource.CACHE, ::cachedSource),
                    Pair(ScriptSource.BUILTIN, ::builtinSource),
                    Pair(ScriptSource.WEB, ::webReleaseSource)
                )
            )
        }

    private fun cachedSource(scriptType: ScriptType): Script? {
        val data = ie.cache.load(cacheSection, scriptType.value) ?: return null
        return Script(
            scriptType,
            ScriptVariant.valueOf(data.variant ?: "unknown"),
            ScriptSource.CACHE,
            data.version ?: "unknown",
            data.code
        )
    }

    private fun builtinSource(scriptType: ScriptType): Script? {
        val fileName = scriptFilenames[scriptType] ?: return null
        val code =
            loadScript(fileName, "Failed to read builtin challenge solver ${scriptType.value}")
        return Script(
            scriptType,
            ScriptVariant.UNMINIFIED,
            ScriptSource.BUILTIN,
            scriptVersion,
            code
        )
    }

    @GuardedBy("itself")
    val cache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(30) {
            private val CACHE_LIMIT = 15

            override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean {
                return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
            }
        })

    private fun webReleaseSource(scriptType: ScriptType): Script? {
        val fileName = minScriptFilenames[scriptType] ?: return null
        if (fileName.isNotEmpty()) {
            synchronized(cache) {
                var code = cache[fileName]
                if (code == null) {
                    val url =
                        "https://github.com/$repository/releases/download/$scriptVersion/$fileName"
                    code = ThrottlingParameterUtils.fetch(url, false)
                    Logger.printDebug { "[${tag}] Downloading challenge solver ${scriptType.value} script from $url" }
                    if (code.isNullOrEmpty()) {
                        return null
                    } else {
                        cache[fileName] = code
                        ie.cache.store(cacheSection, scriptType.value, CachedData(code))
                    }
                }
                return Script(
                    scriptType,
                    ScriptVariant.MINIFIED,
                    ScriptSource.WEB,
                    scriptVersion,
                    code
                )
            }
        }
        return null
    }

    // endregion: challenge solver script
}