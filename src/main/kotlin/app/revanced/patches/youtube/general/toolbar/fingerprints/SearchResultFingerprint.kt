package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VoiceSearch
import app.revanced.util.fingerprint.LiteralValueFingerprint

object SearchResultFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    strings = listOf("search_filter_chip_applied", "search_original_chip_query"),
    literalSupplier = { VoiceSearch },
)