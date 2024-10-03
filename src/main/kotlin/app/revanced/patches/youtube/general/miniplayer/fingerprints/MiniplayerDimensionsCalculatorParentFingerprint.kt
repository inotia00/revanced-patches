package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FloatyBarTopMargin
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("SpellCheckingInspection")
internal object MiniplayerDimensionsCalculatorParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literalSupplier = { FloatyBarTopMargin },
)