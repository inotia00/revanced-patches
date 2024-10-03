package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This value is the key for the playback speed overlay value.
 * Deprecated in YouTube v19.18.41+.
 */
internal object SpeedOverlayFloatValueFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.DOUBLE_TO_FLOAT),
    literalSupplier = { 45411328 },
)