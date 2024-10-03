package app.revanced.patches.youtube.player.ambientmode.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object AmbientModeInFullscreenFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { 45389368 },
)