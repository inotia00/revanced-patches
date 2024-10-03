package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.HorizontalCardList
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BreakingNewsFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { HorizontalCardList },
)