package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object VideoEndFingerprint : LiteralValueFingerprint(
    strings = listOf("Attempting to seek during an ad"),
    literalSupplier = { 45368273 },
)