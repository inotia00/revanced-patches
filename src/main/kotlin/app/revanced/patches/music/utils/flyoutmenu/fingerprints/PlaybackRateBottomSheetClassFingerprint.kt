package app.revanced.patches.music.utils.flyoutmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.VarispeedUnavailableTitle
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackRateBottomSheetClassFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literalSupplier = { VarispeedUnavailableTitle },
)