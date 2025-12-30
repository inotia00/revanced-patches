package com.liskovsoft.youtubeapi.app.nsigsolver.impl

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8ScriptExecutionException
import com.liskovsoft.youtubeapi.app.nsigsolver.common.loadScript
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProviderError
import com.liskovsoft.youtubeapi.app.nsigsolver.runtime.JsRuntimeChalBaseJCP
import com.liskovsoft.youtubeapi.app.nsigsolver.runtime.Script
import com.liskovsoft.youtubeapi.app.nsigsolver.runtime.ScriptSource
import com.liskovsoft.youtubeapi.app.nsigsolver.runtime.ScriptType
import com.liskovsoft.youtubeapi.app.nsigsolver.runtime.ScriptVariant
import java.util.concurrent.Callable
import java.util.concurrent.Executors

internal object V8ChallengeProvider : JsRuntimeChalBaseJCP() {
    private val v8NpmLibFilename = listOf(
        "${libPrefix}polyfill.js",
        "${libPrefix}meriyah-6.1.4.min.js",
        "${libPrefix}astring-1.9.0.min.js"
    )
    private val v8Executor = Executors.newSingleThreadExecutor()
    private var v8Runtime: V8? = null

    override fun iterScriptSources(): Sequence<Pair<ScriptSource, (ScriptType) -> Script?>> =
        sequence {
            for ((source, func) in super.iterScriptSources()) {
                if (source == ScriptSource.WEB || source == ScriptSource.BUILTIN)
                    yield(Pair(ScriptSource.BUILTIN, ::v8NpmSource))
                yield(Pair(source, func))
            }
        }

    private fun v8NpmSource(scriptType: ScriptType): Script? {
        if (scriptType != ScriptType.LIB)
            return null
        // V8-specific lib scripts that uses Deno NPM imports
        val code = loadScript(v8NpmLibFilename, "Failed to read v8 challenge solver lib script")
        return Script(scriptType, ScriptVariant.V8_NPM, ScriptSource.BUILTIN, scriptVersion, code)
    }

    override fun runJsRuntime(stdin: String): String {
        warmup()
        return runJS(stdin)
    }

    private fun runJS(stdin: String, warmup: Boolean = false): String {
        try {
            val result = v8Executor.submit(
                Callable { v8Runtime?.executeStringScript(stdin) }
            ).get()

            return if (warmup) "" else if (result.isNullOrEmpty())
                throw JsChallengeProviderError("V8 runtime error: empty response")
                else result
        } catch (e: V8ScriptExecutionException) {
            if (e.message?.contains("Invalid or unexpected token") ?: false)
                ie.cache.clear(cacheSection) // cached data broken?
            if (!warmup) {
                shutDown()
            }
            throw JsChallengeProviderError("V8 runtime error: ${e.message}", e)
        }
    }

    fun shutDown() {
        v8Executor.submit {
            v8Runtime?.release(false)
            v8Runtime = null
        }
        v8Executor.shutdown()
    }

    fun warmup() {
        if (v8Runtime != null) return

        v8Executor.submit {
            v8Runtime = V8.createV8Runtime()
        }
        runJS(constructCommonStdin(), true)
    }
}