package app.revanced.extension.youtube.shared

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import app.revanced.extension.shared.utils.ResourceUtils.ResourceType
import app.revanced.extension.shared.utils.ResourceUtils.getIdentifier
import java.lang.ref.WeakReference

/**
 * default implementation of [PlayerControlsVisibilityObserver]
 *
 * @param activity activity that contains the controls_layout view
 */
class PlayerControlsVisibilityObserverImpl(
    private val activity: Activity
) : PlayerControlsVisibilityObserver {

    /**
     * id of the direct parent of controls_layout, R.id.controls_button_group_layout
     */
    private val controlsLayoutParentId =
        getIdentifier("controls_button_group_layout", ResourceType.ID, activity)

    /**
     * id of R.id.player_control_play_pause_replay_button_touch_area
     */
    private val controlsLayoutId =
        getIdentifier(
            "player_control_play_pause_replay_button_touch_area",
            ResourceType.ID,
            activity
        )

    /**
     * reference to the controls layout view
     */
    private var controlsLayoutView = WeakReference<View>(null)

    /**
     * is the [controlsLayoutView] set to a valid reference of a view?
     */
    private val isAttached: Boolean
        get() {
            val view = controlsLayoutView.get()
            return view != null && view.parent != null
        }

    /**
     * find and attach the controls_layout view if needed
     */
    private fun maybeAttach() {
        if (isAttached) return

        // find parent, then controls_layout view
        // this is needed because there may be two views where id=R.id.controls_layout
        // because why should google confine themselves to their own guidelines...
        activity.findViewById<ViewGroup>(controlsLayoutParentId)?.let { parent ->
            parent.findViewById<View>(controlsLayoutId)?.let {
                controlsLayoutView = WeakReference(it)
            }
        }
    }

    override val playerControlsVisibility: Int
        get() {
            maybeAttach()
            return controlsLayoutView.get()?.visibility ?: View.GONE
        }

    override val arePlayerControlsVisible: Boolean
        get() = playerControlsVisibility == View.VISIBLE
}

/**
 * provides the visibility status of the fullscreen player controls_layout view.
 * this can be used for detecting when the player controls are shown
 */
interface PlayerControlsVisibilityObserver {
    /**
     * current visibility int of the controls_layout view
     */
    val playerControlsVisibility: Int

    /**
     * is the value of [playerControlsVisibility] equal to [View.VISIBLE]?
     */
    val arePlayerControlsVisible: Boolean
}