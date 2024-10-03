package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ContentPill
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ContentPillFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    literalSupplier = { ContentPill },
)