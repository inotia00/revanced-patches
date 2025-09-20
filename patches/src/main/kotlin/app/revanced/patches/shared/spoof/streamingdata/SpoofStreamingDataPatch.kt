package app.revanced.patches.shared.spoof.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.util.FilesCompat
import app.revanced.util.ResourceGroup
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.cloneMutable
import app.revanced.util.copyResources
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getNode
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.inputStreamFromBundledResourceOrThrow
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

private lateinit var context: ResourcePatchContext
var playerLoadingViewThin = -1L
    private set

private val spoofStreamingDataRawResourcePatch = rawResourcePatch(
    description = "spoofStreamingDataRawResourcePatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        context = this

        playerLoadingViewThin = getResourceId(ID, "player_loading_view_thin")
    }
}

const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofStreamingDataPatch;"
const val EXTENSION_STREAMING_DATA_OUTER_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/StreamingDataOuterClassPatch;"
const val EXTENSION_STREAMING_DATA_INTERFACE =
    "$SPOOF_PATH/StreamingDataOuterClassPatch${'$'}StreamingDataMessage;"

const val EXTENSION_YOUTUBE_SPOOF_PATH =
    app.revanced.patches.youtube.utils.extension.Constants.SPOOF_PATH
const val EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/AudioTrackPatch;"
const val EXTENSION_AUTO_TRACK_BUTTON_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ui/AudioTrackButton;"
const val EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ReloadVideoPatch;"
const val EXTENSION_RELOAD_VIDEO_BUTTON_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ui/ReloadVideoButton;"

fun spoofStreamingDataPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    isYouTube: BytecodePatchBuilder.() -> Boolean = { false },
    outlineIcon: BytecodePatchBuilder.() -> Boolean = { false },
    fixMediaFetchHotConfigChanges: BytecodePatchBuilder.() -> Boolean = { false },
    fixMediaFetchHotConfigAlternativeChanges: BytecodePatchBuilder.() -> Boolean = { false },
    fixParsePlaybackResponseFeatureFlag: BytecodePatchBuilder.() -> Boolean = { false },
    executeBlock: BytecodePatchContext.() -> Unit = {},
    finalizeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    description = "spoofStreamingDataPatch",
) {
    block()

    dependsOn(spoofStreamingDataRawResourcePatch)

    execute {

        // region Block /initplayback requests to fall back to /get_watch requests.

        buildInitPlaybackRequestFingerprint.methodOrThrow().apply {
            val index = indexOfUriToStringInstruction(this) + 1
            val register =
                getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                    """,
            )
        }

        // endregion

        // region Block /get_watch requests to fall back to /player requests.

        buildPlayerRequestURIFingerprint.methodOrThrow().apply {
            val invokeToStringIndex = indexOfUriToStringInstruction(this)
            val uriRegister =
                getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

            addInstructions(
                invokeToStringIndex, """
                    invoke-static { v$uriRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                    move-result-object v$uriRegister
                    """,
            )
        }

        // endregion

        // region Remove /videoplayback request body to fix playback.

        buildMediaDataSourceFingerprint.methodOrThrow().apply {
            val targetIndex = instructions.lastIndex

            addInstructions(
                targetIndex,
                """
                    # Field a: Stream uri.
                    # Field c: Http method.
                    # Field d: Post data.
                    move-object/from16 v0, p0
                    iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                    iget v2, v0, $definingClass->c:I
                    iget-object v3, v0, $definingClass->d:[B
                    invoke-static { v1, v2, v3 }, $EXTENSION_CLASS_DESCRIPTOR->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                    move-result-object v1
                    iput-object v1, v0, $definingClass->d:[B
                    """,
            )
        }

        // endregion

        // region Replace the streaming data.

        createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
            .let { result ->
                result.method.apply {
                    val parseResponseProtoMethodName = "parseFrom"

                    val setStreamDataMethodName = "patch_setStreamingData"
                    val resultClassDef = result.classDef
                    val resultMethodType = resultClassDef.type
                    val setStreamingDataIndex = result.patternMatch!!.startIndex
                    val setStreamingDataRegister =
                        getInstruction<TwoRegisterInstruction>(setStreamingDataIndex).registerA

                    val playerProtoClass =
                        getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                    val protobufClass =
                        protobufClassParseByteBufferFingerprint.definingClassOrThrow()

                    val getStreamingDataField = instructions.find { instruction ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                    }?.getReference<FieldReference>()
                        ?: throw PatchException("Could not find getStreamingDataField")

                    val getVideoDetailsIndex = setStreamingDataIndex + 1
                    val getVideoDetailsFallbackIndex = setStreamingDataIndex + 3
                    val getVideoDetailsField =
                        getInstruction(getVideoDetailsIndex).getReference<FieldReference>()!!
                    val getVideoDetailsFallbackField =
                        getInstruction(getVideoDetailsFallbackIndex).getReference<FieldReference>()!!
                    val freeRegister = implementation!!.registerCount - parameters.size - 2

                    val audioTrackSmaliInstruction = if (isYouTube()) {
                        """
                            invoke-static { v$freeRegister, v$setStreamingDataRegister }, $EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;${STREAMING_DATA_OUTER_CLASS})V
                        """
                    } else {
                        ""
                    }

                    addInstructionsAtControlFlowLabel(
                        setStreamingDataIndex, """
                            iget-object v$freeRegister, p1, $getVideoDetailsField
                            if-nez v$freeRegister, :ignore
                            sget-object v$freeRegister, $getVideoDetailsFallbackField
                            :ignore
                            iget-object v$freeRegister, v$freeRegister, ${getVideoDetailsField.type}->c:Ljava/lang/String;
                            """ + audioTrackSmaliInstruction + """
                            invoke-direct { p0, v$setStreamingDataRegister, v$freeRegister }, $resultMethodType->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                            move-result-object v$setStreamingDataRegister
                            """
                    )

                    result.classDef.methods.filter { method ->
                        MethodUtil.isConstructor(method)
                    }.forEach { method ->
                        method.addInstruction(
                            1,
                            "invoke-static { p0 }, $EXTENSION_STREAMING_DATA_OUTER_CLASS_DESCRIPTOR->initialize($EXTENSION_STREAMING_DATA_INTERFACE)V"
                        )
                    }

                    resultClassDef.interfaces.add(EXTENSION_STREAMING_DATA_INTERFACE)

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            parseResponseProtoMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/nio/ByteBuffer;",
                                    annotations,
                                    "responseProto"
                                )
                            ),
                            STREAMING_DATA_OUTER_CLASS,
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(5),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Check buffer is not null.
                                    if-nez p1, :parse
                                    const/4 v0, 0x0
                                    return-object v0
                                    :parse

                                    # Parse streaming data.
                                    sget-object v0, $playerProtoClass->a:$playerProtoClass
                                    invoke-static { v0, p1 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                    move-result-object v0
                                    check-cast v0, $playerProtoClass
                                    iget-object v0, v0, $getStreamingDataField
                                    return-object v0
                                    """,
                            )
                        },
                    )

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            setStreamDataMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    STREAMING_DATA_OUTER_CLASS,
                                    annotations,
                                    "streamingDataOuterClass"
                                ),
                                ImmutableMethodParameter(
                                    "Ljava/lang/String;",
                                    annotations,
                                    "videoId"
                                )
                            ),
                            STREAMING_DATA_OUTER_CLASS,
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(5),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Get streaming data.
                                    invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                                    move-result-object v0
                                    if-eqz v0, :ignore                                    
                                    return-object v0
                                    :ignore
                                    return-object p1
                                    """,
                            )
                        },
                    )
                }
            }

        videoStreamingDataConstructorFingerprint.matchOrThrow(
            videoStreamingDataToStringFingerprint
        ).let {
            it.method.apply {
                val adaptiveFormatsFieldIndex =
                    indexOfGetAdaptiveFormatsFieldInstruction(this)
                val adaptiveFormatsRegister =
                    getInstruction<TwoRegisterInstruction>(adaptiveFormatsFieldIndex).registerA
                val videoIdIndex =
                    indexOfFirstInstructionReversedOrThrow(adaptiveFormatsFieldIndex) {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IGET_OBJECT &&
                                reference?.type == "Ljava/lang/String;" &&
                                reference.definingClass == definingClass
                    }
                var definingClassRegister =
                    getInstruction<TwoRegisterInstruction>(videoIdIndex).registerB
                val videoIdReference =
                    getInstruction<ReferenceInstruction>(videoIdIndex).reference as FieldReference

                if (isYouTube()) {
                    val setAdaptiveFormatsMethodName = "patch_setAdaptiveFormats"

                    it.classDef.methods.add(
                        ImmutableMethod(
                            definingClass,
                            setAdaptiveFormatsMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/util/List;",
                                    annotations,
                                    "adaptiveFormats"
                                )
                            ),
                            "Ljava/util/List;",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                # Get video id.
                                iget-object v0, p0, $videoIdReference
                                
                                # Override adaptive formats.
                                invoke-static { v0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->prioritizeVideoQuality(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
                                move-result-object p1

                                return-object p1
                                """,
                            )
                        },
                    )

                    addInstructions(
                        adaptiveFormatsFieldIndex + 1, """
                            # Override adaptive formats.
                            invoke-direct { v$definingClassRegister,  v$adaptiveFormatsRegister }, $definingClass->$setAdaptiveFormatsMethodName(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$adaptiveFormatsRegister
                            """
                    )
                }

                val setStreamDataMethodName = "patch_setStreamingData"
                val streamingDataIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT_OBJECT &&
                            reference?.type == STREAMING_DATA_OUTER_CLASS &&
                            reference.definingClass == definingClass
                }
                val streamingDataField =
                    getInstruction<ReferenceInstruction>(streamingDataIndex).reference
                val streamingDataRegister =
                    getInstruction<TwoRegisterInstruction>(streamingDataIndex).registerA

                val setVideoIdIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == videoIdReference
                }
                val setVideoIdRegister =
                    getInstruction<TwoRegisterInstruction>(setVideoIdIndex).registerA
                definingClassRegister =
                    getInstruction<TwoRegisterInstruction>(setVideoIdIndex).registerB

                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        setStreamDataMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                STREAMING_DATA_OUTER_CLASS,
                                annotations,
                                "streamingDataOuterClass"
                            ),
                            ImmutableMethodParameter(
                                "Ljava/lang/String;",
                                annotations,
                                "videoId"
                            )
                        ),
                        STREAMING_DATA_OUTER_CLASS,
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(6),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                move-object/from16 v0, p0
                                # Get streaming data.
                                invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                                move-result-object v1
                                if-eqz v1, :ignore
                                iput-object v1, v0, $streamingDataField
                                return-object v1
                                :ignore
                                return-object p1
                                """,
                        )
                    },
                )

                addInstructions(
                    setVideoIdIndex + 1, """
                        invoke-direct { v$definingClassRegister, v$streamingDataRegister, v$setVideoIdRegister }, $definingClass->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                        move-result-object v$streamingDataRegister
                        """
                )
            }
        }

        // endregion

        // region JavaScript client

        // Some classes that exist in YouTube / YouTube Music are not merged.
        // Instead of relocating all classes in the extension,
        // Only the methods necessary for the patch to function are copied.
        // Note: Protobuf library included with YouTube / YouTube Music seems to have been released before 2016.
        getEmptyRegistryFingerprint.matchOrThrow().let {
            it.classDef.methods.add(
                it.method.cloneMutable(name = "getEmptyRegistry")
            )
        }

        findMethodOrThrow(EXTENSION_UTILS_CLASS_DESCRIPTOR) {
            name == "setContext"
        }.apply {
            addInstruction(
                implementation!!.instructions.lastIndex,
                "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->initializeJavascript()V"
            )
        }

        val (brotliInputStreamClassName, brotliInputStreamMethodCall) = with(
            brotliInputStreamFingerprint.methodOrThrow()
        ) {
            Pair(definingClass, methodCall())
        }

        findMethodOrThrow(EXTENSION_UTILS_CLASS_DESCRIPTOR) {
            name == "getBrotliInputStream"
        }.addInstructions(
            0, """
                new-instance v0, $brotliInputStreamClassName
                invoke-direct {v0, p0}, $brotliInputStreamMethodCall
                return-object v0
                """
        )

        // Copy the j2v8 library.
        with(context) {
            setOf(
                "arm64-v8a",
                "armeabi-v7a",
                "x86",
                "x86_64"
            ).forEach { arch ->
                val architectureDirectory = get("lib/$arch")

                if (architectureDirectory.exists()) {
                    val inputStream = inputStreamFromBundledResourceOrThrow(
                        "shared/spoof/jniLibs",
                        "$arch/libj2v8.so"
                    )
                    FilesCompat.copy(
                        inputStream,
                        architectureDirectory.resolve("libj2v8.so"),
                    )
                }
            }

            arrayOf(
                ResourceGroup(
                    "raw",
                    "po_token.html",
                )
            ).forEach { resourceGroup ->
                copyResources("shared/spoof/shared", resourceGroup)
            }

            document("AndroidManifest.xml").use { document ->
                // WebViewUtils.
                val feature = document.createElement("uses-feature").apply {
                    setAttribute("android:name", "android.hardware.usb.host")
                }
                document.getNode("manifest").appendChild(feature)

                // Fix compile error in YouTube Music.
                val applicationNode =
                    document
                        .getElementsByTagName("application")
                        .item(0) as Element
                applicationNode.setAttribute("android:extractNativeLibs", "true")
            }
        }

        // endregion

        // region Player buttons

        if (isYouTube()) {
            val directory = if (outlineIcon())
                "outline"
            else
                "default"

            arrayOf(
                ResourceGroup(
                    "drawable",
                    "revanced_audio_track.xml",
                    "revanced_reload_video.xml",
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/spoof/$directory", resourceGroup)
            }

            progressBarVisibilityFingerprint
                .methodOrThrow(progressBarVisibilityParentFingerprint).apply {
                    val index = indexOfProgressBarVisibilityInstruction(this)
                    val register = getInstruction<FiveRegisterInstruction>(index).registerD

                    addInstructionsAtControlFlowLabel(
                        index,
                        "invoke-static {v$register}, $EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR->setProgressBarVisibility(I)V"
                    )
                }
        }

        // endregion

        // region Append spoof info.

        nerdsStatsFormatBuilderFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
            }
        }

        // endregion

        // region Disable SABR playback.
        // If SABR is disabled, it seems 'MediaFetchHotConfig' may no longer need to be overridden, but I'm not sure.

        val (mediaFetchEnumClass, sabrFieldReference) =
            with (mediaFetchEnumConstructorFingerprint.methodOrThrow()) {
                val mediaFetchEnumClass = definingClass
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow(DISABLED_BY_SABR_STREAMING_URI_STRING)
                val sabrFieldIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    opcode == Opcode.SPUT_OBJECT &&
                            getReference<FieldReference>()?.type == mediaFetchEnumClass
                }

                Pair(
                    mediaFetchEnumClass,
                    getInstruction<ReferenceInstruction>(sabrFieldIndex).reference
                )
            }

        // The method pattern is slightly different in YouTube Music 6.20.51.
        // TODO: If support for YouTube Music 6.20.51 is dropped, implement this as a generic fingerprint.
        val getMediaFetchEnumFingerprint = legacyFingerprint(
            name = "getMediaFetchEnumFingerprint",
            returnType = mediaFetchEnumClass,
            opcodes = listOf(
                Opcode.SGET_OBJECT,
                Opcode.RETURN_OBJECT,
            ),
            customFingerprint = { method, _ ->
                !method.parameterTypes.isEmpty()
            }
        )

        getMediaFetchEnumFingerprint
            .methodOrThrow()
            .addInstructionsWithLabels(
                0, """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->disableSABR()Z
                    move-result v0
                    if-eqz v0, :ignore
                    sget-object v0, $sabrFieldReference
                    return-object v0
                    :ignore
                    nop
                    """
            )

        // endregion

        // region Fix iOS livestream current time.

        hlsCurrentTimeFingerprint.injectLiteralInstructionBooleanCall(
            HLS_CURRENT_TIME_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->fixHLSCurrentTime(Z)Z"
        )

        // endregion

        // region Skip response encryption in OnesiePlayerRequest

        if (fixMediaFetchHotConfigChanges()) {
            mediaFetchHotConfigFingerprint.injectLiteralInstructionBooleanCall(
                MEDIA_FETCH_HOT_CONFIG_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->useMediaFetchHotConfigReplacement(Z)Z"
            )
        }

        // In 20.14 the flag was merged with 19.50 start playback flag.
        if (fixMediaFetchHotConfigAlternativeChanges()) {
            mediaFetchHotConfigAlternativeFingerprint.injectLiteralInstructionBooleanCall(
                MEDIA_FETCH_HOT_CONFIG_ALTERNATIVE_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->useMediaFetchHotConfigReplacement(Z)Z"
            )
        }

        if (fixParsePlaybackResponseFeatureFlag()) {
            playbackStartDescriptorFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PLAYBACK_START_CHECK_ENDPOINT_USED_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->usePlaybackStartFeatureFlag(Z)Z"
            )
        }

        // endregion

        var patchStatusArray = arrayOf(
            "SpoofStreamingData"
        )

        if (isYouTube()) {
            patchStatusArray += "SpoofStreamingDataYouTube"
        }

        patchStatusArray.forEach { methodName ->
            findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
                name == methodName
            }.returnEarly(true)
        }

        executeBlock()
    }

    if (finalizeBlock != {}) {
        finalize {
            finalizeBlock()
        }
    }
}
