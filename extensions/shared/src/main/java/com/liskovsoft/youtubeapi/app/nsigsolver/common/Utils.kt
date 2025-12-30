package com.liskovsoft.youtubeapi.app.nsigsolver.common

import app.revanced.extension.shared.utils.ResourceUtils.getRawResource
import app.revanced.extension.shared.utils.Utils
import com.liskovsoft.youtubeapi.app.nsigsolver.impl.V8ChallengeProvider.libPrefix

internal open class ScriptLoaderError(message: String, cause: Exception? = null) :
    Exception(message, cause)

internal fun loadScript(filename: String, errorMsg: String? = null): String {
    Utils.getContext() ?: throw ScriptLoaderError(formatError(errorMsg, "Context isn't available"))

    val fixedFilename =
        filename.replace(libPrefix, "")
            .replace(".js", "")

    return getRawResource(fixedFilename)
}

internal fun loadScript(filenames: List<String>, errorMsg: String? = null): String {
    return buildString {
        for (filename in filenames) {
            append(loadScript(filename, errorMsg))
        }
    }
}

internal fun formatError(firstMsg: String?, secondMsg: String) =
    firstMsg?.let { "$it: $secondMsg" } ?: secondMsg
