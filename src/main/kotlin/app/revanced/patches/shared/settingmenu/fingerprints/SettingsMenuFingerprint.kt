package app.revanced.patches.shared.settingmenu.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SettingsMenuFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    literalSupplier = { 16908351 },
)