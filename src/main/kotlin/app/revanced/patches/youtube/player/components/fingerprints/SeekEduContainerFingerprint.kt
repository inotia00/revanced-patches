package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.EasySeekEduContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SeekEduContainerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { EasySeekEduContainer },
)