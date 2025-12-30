package app.revanced.patches.shared.spoof.guide

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val guideEndpointConstructorFingerprint = legacyFingerprint(
    name = "guideEndpointConstructorFingerprint",
    returnType = "V",
    strings = listOf("guide"),
    customFingerprint = { method, _ ->
        method.name == "<init>"
    }
)

internal val guideEndpointRequestBodyFingerprint = legacyFingerprint(
    name = "guideEndpointRequestBodyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.RETURN_VOID),
)

