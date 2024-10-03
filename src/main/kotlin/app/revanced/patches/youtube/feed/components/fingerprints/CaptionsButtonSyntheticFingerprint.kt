package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CaptionToggleContainer
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object CaptionsButtonSyntheticFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.BRIDGE or AccessFlags.SYNTHETIC,
    parameters = listOf("Landroid/content/Context;"),
    literalSupplier = { CaptionToggleContainer },
)