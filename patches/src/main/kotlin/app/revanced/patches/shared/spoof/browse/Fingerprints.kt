package app.revanced.patches.shared.spoof.browse

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val browseEndpointConstructorFingerprint = legacyFingerprint(
    name = "browseEndpointConstructorFingerprint",
    returnType = "V",
    strings = listOf("browse"),
    customFingerprint = { method, _ ->
        method.name == "<init>" &&
                method.indexOfFirstInstructionReversed {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"
                } >= 0
    }
)

internal val browseEndpointRequestBodyFingerprint = legacyFingerprint(
    name = "browseEndpointRequestBodyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
    ),
)

