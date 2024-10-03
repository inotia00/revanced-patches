package app.revanced.patches.music.account.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object AccountSwitcherAccessibilityLabelFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literalSupplier = { AccountSwitcherAccessibility },
)