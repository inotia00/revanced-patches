package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelForcedMuteButton
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShortsPivotLegacyFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("Z", "Z", "L"),
    literalSupplier = { ReelForcedMuteButton },
)