package app.revanced.patches.music.flyoutmenu.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TouchOutside
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object TouchOutsideFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    literalSupplier = { TouchOutside },
)