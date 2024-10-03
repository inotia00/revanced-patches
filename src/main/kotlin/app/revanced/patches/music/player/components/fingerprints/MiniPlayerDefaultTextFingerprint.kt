package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerDefaultText
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object MiniPlayerDefaultTextFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.SGET_OBJECT,
        Opcode.IF_NE
    ),
    literalSupplier = { MiniPlayerDefaultText },
)