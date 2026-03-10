package app.revanced.patches.youtube.general.components

import app.revanced.patches.youtube.utils.resourceid.accountSwitcherAccessibility
import app.revanced.patches.youtube.utils.resourceid.compactLink
import app.revanced.patches.youtube.utils.resourceid.compactListItem
import app.revanced.patches.youtube.utils.resourceid.editSettingsAction
import app.revanced.patches.youtube.utils.resourceid.fab
import app.revanced.patches.youtube.utils.resourceid.pairWithTVKey
import app.revanced.patches.youtube.utils.resourceid.toolTipContentView
import app.revanced.patches.youtube.utils.resourceid.ytCallToAction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val accountListFingerprint = legacyFingerprint(
    name = "accountListFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    literals = listOf(ytCallToAction),
)

internal val accountListParentFingerprint = legacyFingerprint(
    name = "accountListParentFingerprint",
    literals = listOf(compactListItem),
)

internal val accountMenuFingerprint = legacyFingerprint(
    name = "accountMenuFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.AND_INT_LIT16
    )
)

internal val accountMenuParentFingerprint = legacyFingerprint(
    name = "accountMenuParentFingerprint",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(compactLink),
)

internal val accountSwitcherAccessibilityLabelFingerprint = legacyFingerprint(
    name = "accountSwitcherAccessibilityLabelFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literals = listOf(accountSwitcherAccessibility),
)

internal val appBlockingCheckResultToStringFingerprint = legacyFingerprint(
    name = "appBlockingCheckResultToStringFingerprint",
    returnType = "Ljava/lang/String;",
    strings = listOf("AppBlockingCheckResult{intent=")
)

internal val floatingMicrophoneFingerprint = legacyFingerprint(
    name = "floatingMicrophoneFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    ),
    literals = listOf(fab),
)

internal val pipNotificationFingerprint = legacyFingerprint(
    name = "pipNotificationFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(editSettingsAction),
)

internal val preferenceScreenFingerprint = legacyFingerprint(
    name = "preferenceScreenFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf(":android:show_fragment_args"),
    customFingerprint = { method, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags) &&
                indexOfPreferenceScreenInstruction(method) >= 0
    }
)

internal fun indexOfPreferenceScreenInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.returnType == "Landroidx/preference/PreferenceScreen;" &&
                reference.parameterTypes.isEmpty()
    }

internal val preferencePairWithTVFingerprint = legacyFingerprint(
    name = "preferencePairWithTVFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    literals = listOf(pairWithTVKey),
    customFingerprint = { method, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags)
    }
)

internal val tooltipContentFullscreenFingerprint = legacyFingerprint(
    name = "tooltipContentFullscreenFingerprint",
    returnType = "V",
    literals = listOf(45384061L),
)

internal val tooltipContentViewFingerprint = legacyFingerprint(
    name = "tooltipContentViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(toolTipContentView),
)

internal const val TRANSLUCENT_STATUS_BAR_PRIMARY_FEATURE_FLAG = 45400535L
internal const val TRANSLUCENT_STATUS_BAR_SECONDARY_FEATURE_FLAG = 45632194L

internal val translucentStatusBarPrimaryFeatureFlagFingerprint = legacyFingerprint(
    name = "translucentStatusBarPrimaryFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(TRANSLUCENT_STATUS_BAR_PRIMARY_FEATURE_FLAG)
)

internal val translucentStatusBarSecondaryFeatureFlagFingerprint = legacyFingerprint(
    name = "translucentStatusBarSecondaryFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(TRANSLUCENT_STATUS_BAR_SECONDARY_FEATURE_FLAG)
)

