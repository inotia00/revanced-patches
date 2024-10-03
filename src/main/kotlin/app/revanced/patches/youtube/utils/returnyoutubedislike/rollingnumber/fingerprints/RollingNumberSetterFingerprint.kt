package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is compatible with YouTube v18.29.38+
 */
internal object RollingNumberSetterFingerprint : LiteralValueFingerprint(
    opcodes = listOf(Opcode.CHECK_CAST),
    literalSupplier = { 45427773 },
)