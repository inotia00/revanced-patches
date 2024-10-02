package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ContentPillFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Controller must be initialized for a feed before the content pill can be shown.")
)