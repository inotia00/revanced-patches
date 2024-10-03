package app.revanced.patches.music.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BackgroundPlaybackManagerFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    literalSupplier = { 64657230 },
)