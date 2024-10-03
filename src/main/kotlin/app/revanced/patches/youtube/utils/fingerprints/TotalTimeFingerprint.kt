package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TotalTime
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object TotalTimeFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { TotalTime },
)