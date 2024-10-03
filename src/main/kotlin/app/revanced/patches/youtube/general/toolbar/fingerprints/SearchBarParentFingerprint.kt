package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VoiceSearch
import app.revanced.util.fingerprint.LiteralValueFingerprint

object SearchBarParentFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    strings = listOf("voz-target-id"),
    literalSupplier = { VoiceSearch },
)