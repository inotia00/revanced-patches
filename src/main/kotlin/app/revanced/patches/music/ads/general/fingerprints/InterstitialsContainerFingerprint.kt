package app.revanced.patches.music.ads.general.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object InterstitialsContainerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    strings = listOf("overlay_controller_param"),
    literalSupplier = { InterstitialsContainer },
)