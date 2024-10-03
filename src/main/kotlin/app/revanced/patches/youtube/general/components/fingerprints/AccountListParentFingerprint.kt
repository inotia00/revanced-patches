package app.revanced.patches.youtube.general.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CompactListItem
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object AccountListParentFingerprint : LiteralValueFingerprint(
    literalSupplier = { CompactListItem },
)