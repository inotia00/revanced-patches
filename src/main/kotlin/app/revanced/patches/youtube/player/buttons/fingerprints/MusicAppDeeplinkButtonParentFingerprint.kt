package app.revanced.patches.youtube.player.buttons.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MusicAppDeeplinkButtonView
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object MusicAppDeeplinkButtonParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { MusicAppDeeplinkButtonView },
)