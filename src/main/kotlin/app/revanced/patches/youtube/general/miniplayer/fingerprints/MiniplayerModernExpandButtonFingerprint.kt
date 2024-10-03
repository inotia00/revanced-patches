package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerExpand
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Resolves using the class found in [MiniplayerModernViewParentFingerprint].
 */
@Suppress("SpellCheckingInspection")
internal object MiniplayerModernExpandButtonFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literalSupplier = { ModernMiniPlayerExpand },
)