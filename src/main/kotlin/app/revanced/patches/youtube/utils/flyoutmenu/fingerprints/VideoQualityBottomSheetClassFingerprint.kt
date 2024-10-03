package app.revanced.patches.youtube.utils.flyoutmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VideoQualityUnavailableAnnouncement
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object VideoQualityBottomSheetClassFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    literalSupplier = { VideoQualityUnavailableAnnouncement },
)