package app.revanced.patches.youtube.swipe.controls.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * This fingerprint is compatible with YouTube v19.19.39+
 */
internal object SwipeToSwitchVideoFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { 45631116 },
)