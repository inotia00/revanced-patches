package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ChannelListSubMenu
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object ChannelListSubMenuFingerprint : LiteralValueFingerprint(
    literalSupplier = { ChannelListSubMenu },
)