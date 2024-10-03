package app.revanced.patches.music.utils.sponsorblock.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InlineTimeBarAdBreakMarkerColor
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SeekBarConstructorFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { InlineTimeBarAdBreakMarkerColor },
)