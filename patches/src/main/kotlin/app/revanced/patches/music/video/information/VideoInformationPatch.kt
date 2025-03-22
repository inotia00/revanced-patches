package app.revanced.patches.music.video.information

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.utils.extension.Constants.SHARED_PATH
import app.revanced.patches.music.utils.playbackSpeedFingerprint
import app.revanced.patches.music.utils.playbackSpeedParentFingerprint
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.video.playerresponse.Hook
import app.revanced.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import app.revanced.patches.shared.videoLengthFingerprint
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

private const val REGISTER_PLAYER_RESPONSE_MODEL = 4

private const val REGISTER_VIDEO_ID = 0
private const val REGISTER_VIDEO_LENGTH = 1

@Suppress("unused")
private const val REGISTER_VIDEO_LENGTH_DUMMY = 2

private lateinit var PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR: String
private lateinit var videoIdMethodCall: String
private lateinit var videoLengthMethodCall: String

private lateinit var videoInformationMethod: MutableMethod

/**
 * Used in [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint].
 * Since both classes are inherited from the same class,
 * [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint] always have the same [seekSourceEnumType] and [seekSourceMethodName].
 */
private var seekSourceEnumType = ""
private var seekSourceMethodName = ""

private lateinit var playerConstructorMethod: MutableMethod
private var playerConstructorInsertIndex = -1

private lateinit var mdxConstructorMethod: MutableMethod
private var mdxConstructorInsertIndex = -1

private lateinit var videoTimeConstructorMethod: MutableMethod
private var videoTimeConstructorInsertIndex = 2

val videoInformationPatch = bytecodePatch(
    description = "videoInformationPatch",
) {
    dependsOn(
        playerResponseMethodHookPatch,
        sharedResourceIdPatch
    )

    execute {
        fun addSeekInterfaceMethods(
            targetClass: MutableClass,
            targetMethod: MutableMethod,
            seekMethodName: String,
            methodName: String,
            fieldName: String
        ) {
            targetMethod.apply {
                targetClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # first enum (field a) is SEEK_SOURCE_UNKNOWN
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, $definingClass->$seekMethodName(J$seekSourceEnumType)Z
                                move-result p1
                                return p1
                                """.toInstructions(),
                            null,
                            null
                        )
                    ).toMutable()
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0, p1}, $definingClass->seekTo(J)Z
                        move-result v0
                        return v0
                        :ignore
                        const/4 v0, 0x0
                        return v0
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    methodName,
                    fieldName,
                    definingClass,
                    smaliInstructions
                )
            }
        }

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(returnType: String): String {
            methodOrThrow().apply {
                val targetReference = getInstruction<ReferenceInstruction>(
                    indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                                reference.returnType == returnType
                    }
                ).reference

                return "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
            }
        }

        videoEndFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                playerConstructorMethod = it
                playerConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the player controller for use through extension
            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            seekSourceEnumType = parameterTypes[1].toString()
            seekSourceMethodName = name

            // Create extension interface methods.
            addSeekInterfaceMethods(
                videoEndFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideVideoTime",
                "videoInformationClass"
            )
        }

        mdxPlayerDirectorSetVideoStageFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                mdxConstructorMethod = it
                mdxConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the MDX director for use through extension
            onCreateHookMdx(EXTENSION_CLASS_DESCRIPTOR, "initializeMdx")

            // Create extension interface methods.
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideMDXVideoTime",
                "videoInformationMDXClass"
            )
        }

        /**
         * Set current video information
         */
        videoIdFingerprint.matchOrThrow().let {
            it.method.apply {
                val playerResponseModelIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                            reference?.returnType == "Ljava/lang/String;" &&
                            reference.parameterTypes.isEmpty()
                }

                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
                    getInstruction(playerResponseModelIndex)
                        .getReference<MethodReference>()
                        ?.definingClass
                        ?: throw PatchException("Could not find Player Response Model class")

                videoIdMethodCall =
                    videoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
                videoLengthMethodCall =
                    videoLengthFingerprint.getPlayerResponseInstruction("J")

                videoInformationMethod = getVideoInformationMethod()
                it.classDef.methods.add(videoInformationMethod)

                addInstruction(
                    playerResponseModelIndex + 2,
                    "invoke-direct/range {p0 .. p1}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )
            }
        }

        /**
         * Set the video time method
         */
        playerControllerSetTimeReferenceFingerprint.matchOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex)
        }

        /**
         * Set current video time
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video length
         */
        videoLengthHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoLength(J)V")

        /**
         * Set current video id
         */
        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
        addPlayerResponseMethodHook(
            Hook.VideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;)V"
            ),
        )
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
        )
        /**
         * Hook current playback speed
         */
        playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let {
            it.getWalkerMethod(it.patternMatch!!.endIndex).apply {
                addInstruction(
                    implementation!!.instructions.lastIndex,
                    "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                )
            }
        }

        /**
         * Hook current video quality
         */
        videoQualityListFingerprint.matchOrThrow().let {
            it.method.apply {
                val videoQualityMethodName =
                    findMethodOrThrow(definingClass) { parameterTypes.first() == "I" }.name
                // set video quality array
                val listIndex = it.patternMatch!!.startIndex
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoQualityList([Ljava/lang/Object;)V"
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0}, $definingClass->$videoQualityMethodName(I)V
                        :ignore
                        return-void
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    "overrideVideoQuality",
                    "videoQualityClass",
                    definingClass,
                    smaliInstructions
                )

            }
        }

        // set current video quality
        videoQualityTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val textIndex = it.patternMatch!!.endIndex
                val textRegister = getInstruction<TwoRegisterInstruction>(textIndex).registerA

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoQuality(Ljava/lang/String;)V"
                )
            }
        }
    }
}

private fun MutableMethod.getVideoInformationMethod(): MutableMethod =
    ImmutableMethod(
        definingClass,
        "setVideoInformation",
        listOf(
            ImmutableMethodParameter(
                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR,
                annotations,
                null
            )
        ),
        "V",
        AccessFlags.PRIVATE or AccessFlags.FINAL,
        annotations,
        null,
        ImmutableMethodImplementation(
            REGISTER_PLAYER_RESPONSE_MODEL + 1, """
                $videoIdMethodCall
                move-result-object v$REGISTER_VIDEO_ID
                $videoLengthMethodCall
                move-result-wide v$REGISTER_VIDEO_LENGTH
                return-void
                """.toInstructions(),
            null,
            null
        )
    ).toMutable()

private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
    addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
    insert(insertIndex, "p1, p2", descriptor)

/**
 * Hook the player controller.  Called when a video is opened or the current video is changed.
 *
 * Note: This hook is called very early and is called before the video id, video time, video length,
 * and many other data fields are set.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
    playerConstructorMethod.addInstruction(
        playerConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

/**
 * Hook the MDX player director. Called when playing videos while casting to a big screen device.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHookMdx(targetMethodClass: String, targetMethodName: String) =
    mdxConstructorMethod.addInstruction(
        mdxConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

internal fun videoIdHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_ID}, $descriptor"
    )
}

internal fun videoLengthHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_LENGTH, v$REGISTER_VIDEO_LENGTH_DUMMY}, $descriptor"
    )
}

/**
 * Hook the video time.
 * The hook is usually called once per second.
 *
 * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
    videoTimeConstructorMethod.insertTimeHook(
        videoTimeConstructorInsertIndex++,
        "$targetMethodClass->$targetMethodName(J)V"
    )
