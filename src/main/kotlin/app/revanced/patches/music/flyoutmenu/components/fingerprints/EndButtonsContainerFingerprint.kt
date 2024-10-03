package app.revanced.patches.music.flyoutmenu.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.EndButtonsContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object EndButtonsContainerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { EndButtonsContainer },
)

