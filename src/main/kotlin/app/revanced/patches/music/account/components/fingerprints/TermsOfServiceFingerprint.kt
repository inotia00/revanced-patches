package app.revanced.patches.music.account.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TosFooter
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object TermsOfServiceFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    literalSupplier = { TosFooter },
)
