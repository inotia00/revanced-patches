package app.revanced.patches.youtube.player.fullscreen.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.PlayerVideoTitleView
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object PlayerTitleViewFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { PlayerVideoTitleView },
)