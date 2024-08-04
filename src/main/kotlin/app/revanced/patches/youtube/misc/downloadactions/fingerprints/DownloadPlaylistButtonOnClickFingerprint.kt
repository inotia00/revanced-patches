package app.revanced.patches.youtube.misc.downloadactions.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.misc.downloadactions.fingerprints.DownloadPlaylistButtonOnClickFingerprint.PLAYLIST_ON_CLICK_INITIALIZE_PAREMETER
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference


object DownloadPlaylistButtonOnClickFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.parameterTypes == PLAYLIST_ON_CLICK_INITIALIZE_PAREMETER
        } >= 0
    }
) {
    val PLAYLIST_ON_CLICK_INITIALIZE_PAREMETER = listOf(
        "Ljava/lang/String;",
        "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
        "I",
        "Landroid/view/View${'$'}OnClickListener;"
    )
}
