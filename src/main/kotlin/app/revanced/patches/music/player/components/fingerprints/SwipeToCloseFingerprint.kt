package app.revanced.patches.music.player.components.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SwipeToCloseFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45398432 },
)