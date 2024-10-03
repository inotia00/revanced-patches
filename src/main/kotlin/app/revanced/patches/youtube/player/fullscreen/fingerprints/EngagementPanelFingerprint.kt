package app.revanced.patches.youtube.player.fullscreen.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementPanel
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object EngagementPanelFingerprint : LiteralValueFingerprint(
    returnType = "L",
    parameters = listOf("L"),
    literalSupplier = { FullScreenEngagementPanel },
)