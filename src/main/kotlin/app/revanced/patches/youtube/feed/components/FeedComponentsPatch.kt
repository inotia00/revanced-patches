package app.revanced.patches.youtube.feed.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.feed.components.fingerprints.BreakingNewsFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.CaptionsButtonFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.CaptionsButtonSyntheticFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ChannelListSubMenuFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ChannelListSubMenuTabletFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ChannelListSubMenuTabletSyntheticFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ChannelTabBuilderFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ChannelTabRendererFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ContentPillFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ElementParserFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ElementParserParentFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.EngagementPanelUpdateFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.FilterBarHeightFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.LatestVideosButtonFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.LinearLayoutManagerItemCountsFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.RelatedChipCloudFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.SearchResultsChipBarFingerprint
import app.revanced.patches.youtube.feed.components.fingerprints.ShowMoreButtonFingerprint
import app.revanced.patches.youtube.utils.bottomsheet.BottomSheetHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.EngagementPanelBuilderFingerprint
import app.revanced.patches.youtube.utils.fingerprints.ScrollTopParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.FEED_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.FEED_PATH
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.Bar
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CaptionToggleContainer
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ChannelListSubMenu
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ContentPill
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.HorizontalCardList
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.alsoResolve
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.injectLiteralInstructionViewCall
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
object FeedComponentsPatch : BaseBytecodePatch(
    name = "Hide feed components",
    description = "Adds options to hide components related to feeds.",
    dependencies = setOf(
        LithoFilterPatch::class,
        NavigationBarHookPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        BottomSheetHookPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BreakingNewsFingerprint,
        CaptionsButtonFingerprint,
        CaptionsButtonSyntheticFingerprint,
        ChannelListSubMenuFingerprint,
        ChannelListSubMenuTabletFingerprint,
        ChannelListSubMenuTabletSyntheticFingerprint,
        ChannelTabRendererFingerprint,
        ContentPillFingerprint,
        ElementParserParentFingerprint,
        EngagementPanelBuilderFingerprint,
        FilterBarHeightFingerprint,
        LatestVideosButtonFingerprint,
        LinearLayoutManagerItemCountsFingerprint,
        RelatedChipCloudFingerprint,
        ScrollTopParentFingerprint,
        SearchResultsChipBarFingerprint,
        ShowMoreButtonFingerprint,
    )
) {
    private const val CAROUSEL_SHELF_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CarouselShelfFilter;"
    private const val FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/FeedComponentsFilter;"
    private const val FEED_VIDEO_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/FeedVideoFilter;"
    private const val FEED_VIDEO_VIEWS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/FeedVideoViewsFilter;"
    private const val KEYWORD_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/KeywordContentFilter;"
    private const val RELATED_VIDEO_CLASS_DESCRIPTOR =
        "$FEED_PATH/RelatedVideoPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for hide carousel shelf, subscriptions channel section, latest videos button

        listOf(
            // carousel shelf, only used to tablet layout.
            Triple(
                BreakingNewsFingerprint,
                "hideBreakingNewsShelf",
                HorizontalCardList
            ),
            // subscriptions channel section.
            Triple(
                ChannelListSubMenuFingerprint,
                "hideSubscriptionsChannelSection",
                ChannelListSubMenu
            ),
            // latest videos button
            Triple(
                ContentPillFingerprint,
                "hideLatestVideosButton",
                ContentPill
            ),
            Triple(
                LatestVideosButtonFingerprint,
                "hideLatestVideosButton",
                Bar
            ),
        ).forEach { (fingerprint, methodName, literal) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $FEED_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V
                    """
            fingerprint.injectLiteralInstructionViewCall(literal, smaliInstruction)
        }

        // endregion

        // region patch for hide caption button

        CaptionsButtonFingerprint.resultOrThrow().mutableMethod.apply {
            val constIndex = indexOfFirstWideLiteralInstructionValueOrThrow(CaptionToggleContainer)
            val insertIndex = indexOfFirstInstructionReversedOrThrow(constIndex, Opcode.IF_EQZ)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $FEED_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/view/View;)Landroid/view/View;
                    move-result-object v$insertRegister
                    """
            )
        }

        CaptionsButtonSyntheticFingerprint.resultOrThrow().mutableMethod.apply {
            val constIndex = indexOfFirstWideLiteralInstructionValueOrThrow(CaptionToggleContainer)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $FEED_CLASS_DESCRIPTOR->hideCaptionsButtonContainer(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide relative video

        fun Method.indexOfEngagementPanelBuilderInstruction(targetMethod: MutableMethod) =
            indexOfFirstInstruction {
                opcode == Opcode.INVOKE_DIRECT &&
                        MethodUtil.methodSignaturesMatch(
                            targetMethod,
                            getReference<MethodReference>()!!
                        )
            }

        EngagementPanelBuilderFingerprint.resultOrThrow().let {
            it.mutableClass.methods.filter { method ->
                method.indexOfEngagementPanelBuilderInstruction(it.mutableMethod) >= 0
            }.forEach { method ->
                method.apply {
                    val index = indexOfEngagementPanelBuilderInstruction(it.mutableMethod)
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstruction(
                        index + 2,
                        "invoke-static {v$register}, " +
                                "$RELATED_VIDEO_CLASS_DESCRIPTOR->showEngagementPanel(Ljava/lang/Object;)V"
                    )
                }
            }
        }

        EngagementPanelUpdateFingerprint.alsoResolve(
            context, EngagementPanelBuilderFingerprint
        ).mutableMethod.addInstruction(
            0,
            "invoke-static {}, $RELATED_VIDEO_CLASS_DESCRIPTOR->hideEngagementPanel()V"
        )

        // BytecodeUtils.getWalkerMethod must be used here
        // Otherwise, MethodWalker finds the wrong class in YouTube 18.29.38:
        // https://github.com/ReVanced/revanced-patcher/issues/309
        LinearLayoutManagerItemCountsFingerprint.resultOrThrow().let {
            val methodWalker =
                it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)
            methodWalker.apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $RELATED_VIDEO_CLASS_DESCRIPTOR->overrideItemCounts(I)I
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide subscriptions channel section for tablet

        arrayOf(
            ChannelListSubMenuTabletFingerprint,
            ChannelListSubMenuTabletSyntheticFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $FEED_CLASS_DESCRIPTOR->hideSubscriptionsChannelSection()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }

        // endregion

        // region patch for hide category bar

        FilterBarHeightFingerprint.patch<TwoRegisterInstruction> { register ->
            """
                invoke-static { v$register }, $FEED_CLASS_DESCRIPTOR->hideCategoryBarInFeed(I)I
                move-result v$register
            """
        }

        RelatedChipCloudFingerprint.patch<OneRegisterInstruction>(1) { register ->
            "invoke-static { v$register }, " +
                    "$FEED_CLASS_DESCRIPTOR->hideCategoryBarInRelatedVideos(Landroid/view/View;)V"
        }

        SearchResultsChipBarFingerprint.patch<OneRegisterInstruction>(-1, -2) { register ->
            """
                invoke-static { v$register }, $FEED_CLASS_DESCRIPTOR->hideCategoryBarInSearch(I)I
                move-result v$register
            """
        }

        // endregion

        // region patch for hide mix playlists

        ElementParserFingerprint.resolve(
            context,
            ElementParserParentFingerprint.resultOrThrow().classDef
        )
        ElementParserFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 2
                val insertIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    reference?.parameterTypes?.size == 1
                            && reference.parameterTypes.first() == "[B"
                            && reference.returnType.startsWith("L")
                }

                val objectIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_OBJECT)
                val objectRegister = getInstruction<TwoRegisterInstruction>(objectIndex).registerA

                val jumpIndex = it.scanResult.patternScanResult!!.startIndex

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {v$objectRegister, v$freeRegister}, $FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR->filterMixPlaylists(Ljava/lang/Object;[B)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :filter
                        """, ExternalLabel("filter", getInstruction(jumpIndex))
                )

                addInstruction(
                    0,
                    "move-object/from16 v$freeRegister, p3"
                )
            }
        }

        // endregion

        // region patch for hide show more button

        ShowMoreButtonFingerprint.resultOrThrow().let {
            val getViewMethod =
                it.mutableClass.methods.find { method ->
                    method.parameters.isEmpty() &&
                            method.returnType == "Landroid/view/View;"
                }

            getViewMethod?.apply {
                val targetIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $FEED_CLASS_DESCRIPTOR->hideShowMoreButton(Landroid/view/View;)V"
                )
            } ?: throw PatchException("Failed to find getView method")
        }

        // endregion

        // region patch for hide channel tab

        ChannelTabBuilderFingerprint.resolve(
            context,
            ScrollTopParentFingerprint.resultOrThrow().classDef
        )

        val channelTabBuilderMethod = ChannelTabBuilderFingerprint.resultOrThrow().mutableMethod

        ChannelTabRendererFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val iteratorIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "hasNext"
                }
                val iteratorRegister =
                    getInstruction<FiveRegisterInstruction>(iteratorIndex).registerC

                val targetIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_INTERFACE
                            && reference?.returnType == channelTabBuilderMethod.returnType
                            && reference.parameterTypes == channelTabBuilderMethod.parameterTypes
                }

                val objectIndex =
                    indexOfFirstInstructionReversedOrThrow(targetIndex, Opcode.IGET_OBJECT)
                val objectInstruction = getInstruction<TwoRegisterInstruction>(objectIndex)
                val objectReference = getInstruction<ReferenceInstruction>(objectIndex).reference

                addInstructionsWithLabels(
                    objectIndex + 1, """
                        invoke-static {v${objectInstruction.registerA}}, $FEED_CLASS_DESCRIPTOR->hideChannelTab(Ljava/lang/String;)Z
                        move-result v${objectInstruction.registerA}
                        if-eqz v${objectInstruction.registerA}, :ignore
                        invoke-interface {v$iteratorRegister}, Ljava/util/Iterator;->remove()V
                        goto :next_iterator
                        :ignore
                        iget-object v${objectInstruction.registerA}, v${objectInstruction.registerB}, $objectReference
                        """, ExternalLabel("next_iterator", getInstruction(iteratorIndex))
                )
            }
        }

        // endregion

        LithoFilterPatch.addFilter(CAROUSEL_SHELF_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(FEED_VIDEO_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(FEED_VIDEO_VIEWS_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(KEYWORD_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: HIDE_FEED_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun <RegisterInstruction : OneRegisterInstruction> MethodFingerprint.patch(
        insertIndexOffset: Int = 0,
        hookRegisterOffset: Int = 0,
        instructions: (Int) -> String
    ) =
        resultOrThrow().let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex

                val insertIndex = endIndex + insertIndexOffset
                val register =
                    getInstruction<RegisterInstruction>(endIndex + hookRegisterOffset).registerA

                addInstructions(insertIndex, instructions(register))
            }
        }
}
