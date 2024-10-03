package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekEasyHorizontalTouchOffsetToStartScrubbing
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object HorizontalTouchOffsetConstructorFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { SeekEasyHorizontalTouchOffsetToStartScrubbing },
)