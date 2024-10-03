package app.revanced.patches.youtube.player.fullscreen.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * This fingerprint is compatible with YouTube v18.42.41+
 */
internal object LandScapeModeConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    literalSupplier = { 45446428 },
)