package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.player.components.fingerprints.ShuffleOnClickFingerprint.indexOfAccessibilityInstruction
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ShuffleOnClickFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(45468) &&
                methodDef.name == "onClick" &&
                indexOfAccessibilityInstruction(methodDef) >= 0
    }
) {
    fun indexOfAccessibilityInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "announceForAccessibility"
        }
}

