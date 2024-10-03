package app.revanced.patches.music.player.components.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * Deprecated in YouTube Music v6.31.55+
 */
internal object OldPlayerLayoutFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45399578 },
)