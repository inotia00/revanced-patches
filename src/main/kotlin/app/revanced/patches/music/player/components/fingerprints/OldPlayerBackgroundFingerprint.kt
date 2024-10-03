package app.revanced.patches.music.player.components.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * Deprecated in YouTube Music v6.34.51+
 */
internal object OldPlayerBackgroundFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45415319 },
)