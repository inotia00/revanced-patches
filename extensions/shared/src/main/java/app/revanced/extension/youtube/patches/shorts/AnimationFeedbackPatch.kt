package app.revanced.extension.youtube.patches.shorts

import app.revanced.extension.shared.utils.ResourceUtils
import app.revanced.extension.youtube.patches.utils.LottieAnimationViewPatch
import app.revanced.extension.youtube.settings.Settings
import com.airbnb.lottie.LottieAnimationView

@Suppress("unused")
object AnimationFeedbackPatch {
    private val CURRENT_TYPE = Settings.ANIMATION_TYPE.get()

    val pauseTapFeedbackHidden: Int by lazy {
        ResourceUtils.getRawIdentifier("pause_tap_feedback_hidden")
    }

    val playTapFeedbackHidden: Int by lazy {
        ResourceUtils.getRawIdentifier("play_tap_feedback_hidden")
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun getShortsLikeFeedbackId(originalId: Int): Int {
        return if (CURRENT_TYPE == AnimationType.ORIGINAL || CURRENT_TYPE.rawRes == 0)
            return originalId
        else CURRENT_TYPE.rawRes
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setShortsLikeFeedback(lottieAnimationView: LottieAnimationView?) {
        if (CURRENT_TYPE != AnimationType.ORIGINAL) {
            lottieAnimationView?.let {
                LottieAnimationViewPatch.setLottieAnimationRawResources(
                    it,
                    CURRENT_TYPE.rawRes
                )
            }
        }
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setShortsPauseFeedback(lottieAnimationView: LottieAnimationView?) {
        if (Settings.HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND.get()) {
            if (pauseTapFeedbackHidden != 0) {
                lottieAnimationView?.let {
                    LottieAnimationViewPatch.setLottieAnimationRawResources(
                        it,
                        pauseTapFeedbackHidden
                    )
                }
            }
        }
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setShortsPlayFeedback(lottieAnimationView: LottieAnimationView?) {
        if (Settings.HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND.get()) {
            if (playTapFeedbackHidden != 0) {
                lottieAnimationView?.let {
                    LottieAnimationViewPatch.setLottieAnimationRawResources(
                        it,
                        playTapFeedbackHidden
                    )
                }
            }
        }
    }

    enum class AnimationType(jsonName: String) {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL(""),
        THUMBS_UP("like_tap_feedback"),
        THUMBS_UP_CAIRO("like_tap_feedback_cairo"),
        HEART("like_tap_feedback_heart"),
        HEART_TINT("like_tap_feedback_heart_tint"),
        HIDDEN("like_tap_feedback_hidden");

        /**
         * Animation id.
         */
        val rawRes: Int = if (jsonName.isNotEmpty())
            ResourceUtils.getRawIdentifier(jsonName)
        else
            0
    }
}
