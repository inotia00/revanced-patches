package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.EndScreenElementLayoutCircle
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object LayoutCircleFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literalSupplier = { EndScreenElementLayoutCircle },
)