package app.revanced.patches.music.flyoutmenu.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TrimSilenceSwitch
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object TrimSilenceSwitchFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literalSupplier = { TrimSilenceSwitch },
)

