package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlineVideoCamera
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object CreateButtonDrawableFingerprint : LiteralValueFingerprint(
    literalSupplier = { YtOutlineVideoCamera },
)