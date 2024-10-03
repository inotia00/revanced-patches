package app.revanced.patches.youtube.utils.bottomsheet.fingerprint

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DesignBottomSheet
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object BottomSheetBehaviorFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { DesignBottomSheet },
)