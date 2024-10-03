package app.revanced.patches.youtube.swipe.controls.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementOverlay
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object FullScreenEngagementOverlayFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { FullScreenEngagementOverlay },
)