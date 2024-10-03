package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomUiContainerStub
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object BottomControlsInflateFingerprint : LiteralValueFingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literalSupplier = { BottomUiContainerStub },
)