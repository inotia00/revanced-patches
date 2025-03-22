package app.revanced.patches.youtube.player.descriptions

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is compatible with YouTube v18.35.xx~
 * Nonetheless, the patch works in YouTube v19.05.xx~
 */
internal val textViewComponentFingerprint = legacyFingerprint(
    name = "textViewComponentFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.CMPL_FLOAT),
    customFingerprint = { method, _ ->
        method.implementation != null &&
                indexOfTextIsSelectableInstruction(method) >= 0
    },
)

internal fun indexOfTextIsSelectableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.name == "setTextIsSelectable" &&
                reference.definingClass != "Landroid/widget/TextView;"
    }