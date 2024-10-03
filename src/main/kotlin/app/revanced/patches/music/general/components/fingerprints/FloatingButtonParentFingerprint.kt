package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FloatingButtonParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    literalSupplier = { 259982244 },
)

