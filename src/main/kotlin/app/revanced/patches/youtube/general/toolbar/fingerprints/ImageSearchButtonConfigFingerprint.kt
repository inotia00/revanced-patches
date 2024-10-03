package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * This fingerprint is compatible with YouTube v19.07.40+
 */
internal object ImageSearchButtonConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literalSupplier = { 45617544 },
)