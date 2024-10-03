package app.revanced.patches.youtube.swipe.controls.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * This fingerprint is compatible with YouTube v18.29.38+
 */
internal object WatchPanelGesturesFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { 45372793 },
)