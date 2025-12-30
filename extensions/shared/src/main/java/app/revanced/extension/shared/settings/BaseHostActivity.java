package app.revanced.extension.shared.settings;

import static app.revanced.extension.shared.utils.ResourceUtils.getIdIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getStringIdentifier;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import app.revanced.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

/**
 * Note that the superclass is overwritten to the superclass of the LicenseMenuActivity at patch time.
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "deprecation"})
public abstract class BaseHostActivity extends Activity {
    private static final int ID_REVANCED_SETTINGS_FRAGMENTS =
            getIdIdentifier("revanced_settings_fragments");
    private static final int ID_REVANCED_TOOLBAR_PARENT =
            getIdIdentifier("revanced_toolbar_parent");
    public static final int LAYOUT_REVANCED_SETTINGS_WITH_TOOLBAR =
            getLayoutIdentifier("revanced_settings_with_toolbar");
    private static final int STRING_REVANCED_SETTINGS_TITLE =
            getStringIdentifier("revanced_settings_title");


    /**
     * Layout parameters for the toolbar, extracted from the dummy toolbar.
     */
    protected static ViewGroup.LayoutParams toolbarLayoutParams;

    protected boolean isInitialized = false;

    /**
     * Sets the layout parameters for the toolbar.
     */
    public static void setToolbarLayoutParams(Toolbar toolbar) {
        if (toolbarLayoutParams != null) {
            toolbar.setLayoutParams(toolbarLayoutParams);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Utils.getLocalizedContext(base));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isInitialized = false;
        try {
            // Check sanity first
            String dataString = getIntent().getDataString();
            if (!"revanced_settings_intent".equals(dataString)) {
                // User did not open RVX Settings
                // (For example, the user opened the Open source licenses menu)
                Logger.printDebug(() -> "onCreate ignored");
                return;
            }

            customizeActivityTheme(this);
            setContentView(getContentViewResourceId());

            PreferenceFragment fragment = createPreferenceFragment();
            createToolbar(fragment);

            getFragmentManager()
                    .beginTransaction()
                    .replace(ID_REVANCED_SETTINGS_FRAGMENTS, fragment)
                    .commit();

            isInitialized = true;
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate failure", ex);
        }
    }

    /**
     * Creates and configures a toolbar for the activity, replacing a dummy placeholder.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    protected void createToolbar(PreferenceFragment fragment) {
        // Replace dummy placeholder toolbar.
        // This is required to fix submenu title alignment issue with Android ASOP 15+
        ViewGroup toolBarParent = findViewById(ID_REVANCED_TOOLBAR_PARENT);
        ViewGroup dummyToolbar = Utils.getChildViewByResourceName(toolBarParent, "revanced_toolbar");
        toolbarLayoutParams = dummyToolbar.getLayoutParams();
        toolBarParent.removeView(dummyToolbar);

        // Sets appropriate system navigation bar color for the activity.
        BaseThemeUtils.setNavigationBarColor(getWindow());

        Toolbar toolbar = new Toolbar(toolBarParent.getContext());
        toolbar.setBackgroundColor(getToolbarBackgroundColor());
        toolbar.setNavigationIcon(getNavigationIcon());
        toolbar.setNavigationOnClickListener(getNavigationClickListener(this));
        toolbar.setTitle(STRING_REVANCED_SETTINGS_TITLE);

        if (isSDKAbove(24)) {
            final int margin = Utils.dipToPixels(16);
            toolbar.setTitleMarginStart(margin);
            toolbar.setTitleMarginEnd(margin);
        }
        TextView toolbarTextView = Utils.getChildView(toolbar, false, view -> view instanceof TextView);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(BaseThemeUtils.getAppForegroundColor());
            toolbarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }
        setToolbarLayoutParams(toolbar);

        onPostToolbarSetup(this, toolbar, fragment);

        toolBarParent.addView(toolbar, 0);
    }

    @Override
    protected void onDestroy() {
        isInitialized = false;
        super.onDestroy();
    }

    /**
     * Injection point.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Customizes the activity's theme.
     */
    protected abstract void customizeActivityTheme(Activity activity);

    /**
     * Returns the resource ID for the content view layout.
     */
    protected abstract int getContentViewResourceId();

    /**
     * Returns the background color for the toolbar.
     */
    protected abstract int getToolbarBackgroundColor();

    /**
     * Returns the navigation icon drawable for the toolbar.
     */
    protected abstract Drawable getNavigationIcon();

    /**
     * Returns the click listener for the toolbar's navigation icon.
     */
    protected abstract View.OnClickListener getNavigationClickListener(Activity activity);

    /**
     * Creates the PreferenceFragment to be injected into the activity.
     */
    protected PreferenceFragment createPreferenceFragment() {
        return new ToolbarPreferenceFragment();
    }

    /**
     * Performs additional setup after the toolbar is configured.
     *
     * @param activity The activity hosting the toolbar.
     * @param toolbar  The configured toolbar.
     * @param fragment The PreferenceFragment associated with the activity.
     */
    protected void onPostToolbarSetup(Activity activity, Toolbar toolbar, PreferenceFragment fragment) {
    }

    protected abstract boolean handleBackPress();
}
