package app.revanced.patches.youtube.feed.flyoutmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.PosterArtWidthDefault
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ContextualMenuItemBuilderFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.ADD_INT_2ADDR
    ),
    literalSupplier = { PosterArtWidthDefault },
)