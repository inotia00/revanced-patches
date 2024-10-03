package app.revanced.patches.music.utils.fix.header.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * This fingerprint is compatible with YouTube Music v7.04.51+
 */
internal object HeaderSwitchConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literalSupplier = { 45617851 },
)