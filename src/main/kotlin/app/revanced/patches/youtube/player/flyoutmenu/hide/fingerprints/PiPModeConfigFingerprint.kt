package app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * This fingerprint is compatible with YouTube v18.39.xx+
 */
internal object PiPModeConfigFingerprint : LiteralValueFingerprint(
    literalSupplier = { 45427407 },
)