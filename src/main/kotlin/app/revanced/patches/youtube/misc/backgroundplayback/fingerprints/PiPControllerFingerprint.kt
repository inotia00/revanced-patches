package app.revanced.patches.youtube.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PiPControllerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.INVOKE_DIRECT
    ),
    literalSupplier = { 151635310 },
)