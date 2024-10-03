package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ExpandButtonDown
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ShowMoreButtonFingerprint : LiteralValueFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literalSupplier = { ExpandButtonDown },
)