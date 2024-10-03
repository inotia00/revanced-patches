package app.revanced.patches.youtube.player.fullscreen.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AppRelatedEndScreenResults
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object RelatedEndScreenResultsFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { AppRelatedEndScreenResults },
)