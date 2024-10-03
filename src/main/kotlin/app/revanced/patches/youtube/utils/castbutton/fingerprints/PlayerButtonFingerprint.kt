package app.revanced.patches.youtube.utils.castbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlayerButtonFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    literalSupplier = { 11208 },
)