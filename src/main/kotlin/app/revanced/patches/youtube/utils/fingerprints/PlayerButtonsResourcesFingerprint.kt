package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object PlayerButtonsResourcesFingerprint : LiteralValueFingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/res/Resources;"),
    literalSupplier = { 17694721 },
)