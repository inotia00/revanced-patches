package app.revanced.patches.youtube.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object KidsBackgroundPlaybackPolicyControllerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "L", "L"),
    literalSupplier = { 5 },
)
