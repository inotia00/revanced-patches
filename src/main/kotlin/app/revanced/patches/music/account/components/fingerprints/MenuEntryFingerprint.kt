package app.revanced.patches.music.account.components.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MenuEntry
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object MenuEntryFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { MenuEntry },
)
