package app.revanced.patches.youtube.general.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object AccountSwitcherAccessibilityLabelFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literalSupplier = { AccountSwitcherAccessibility },
)