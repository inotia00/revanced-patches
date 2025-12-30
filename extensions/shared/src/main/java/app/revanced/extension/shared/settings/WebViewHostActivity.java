package app.revanced.extension.shared.settings;

import static app.revanced.extension.shared.utils.ResourceUtils.getIdIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.ResourceUtils.getMenuIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import app.revanced.extension.shared.innertube.utils.AuthUtils;
import app.revanced.extension.shared.patches.auth.YouTubeAuthPatch;
import app.revanced.extension.shared.ui.CustomDialog;
import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

/**
 * Note that the superclass is overwritten to the superclass of the VrWelcomeActivity at patch time.
 */
@SuppressWarnings({"ExtractMethodRecommender", "deprecation", "unused"})
public class WebViewHostActivity extends Activity {

    protected final String CHROME_VERSION = "140.0.0.0";
    protected final String EMBEDDED_SETUP_URL =
            "https://accounts.google.com/EmbeddedSetup";
    protected final String JS_SCRIPT =
            "(function() { return document.getElementById('profileIdentifier').innerHTML; })();";
    protected final String OAUTH_TOKEN = "oauth_token";
    protected final String USER_AGENT_CHROME_FORMAT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36";
    protected final String USER_AGENT_CHROME_MOBILE_FORMAT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36";

    @NonNull
    protected String userAgent = "";

    @NonNull
    protected String toolbarLabel = "";

    protected Toolbar toolbar;

    protected WebView webView;

    protected boolean isEmbeddedSetup = false;

    protected boolean isInitialized = false;

    protected boolean clearCookiesOnStartUp;

    protected boolean clearCookiesOnShutDown;

    protected boolean useDesktopUserAgent;

    @NonNull
    protected String url = "https://www.youtube.com/signin";

    @NonNull
    protected String cookies = "";

    @NonNull
    protected String dataSyncId = "";

    @NonNull
    protected String visitorData = "";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            isInitialized = false;
            Intent intent = getIntent();
            String dataString = intent.getDataString();
            if (!StringUtils.equals("revanced_webview", dataString)) {
                Logger.printDebug(() -> "onCreate ignored");
                return;
            }
            if (!Utils.isNetworkConnected()) {
                Utils.showToastShort(str("revanced_webview_toast_no_network"));
                return;
            }
            if (!Utils.isWebViewSupported()) {
                Utils.showToastShort(str("revanced_webview_toast_unavailable"));
                return;
            }
            clearCookiesOnStartUp =
                    intent.getBooleanExtra("clearCookiesOnStartUp", false);
            clearCookiesOnShutDown =
                    intent.getBooleanExtra("clearCookiesOnShutDown", false);
            useDesktopUserAgent =
                    intent.getBooleanExtra("useDesktopUserAgent", false);
            userAgent = String.format(
                    useDesktopUserAgent
                            ? USER_AGENT_CHROME_FORMAT
                            : USER_AGENT_CHROME_MOBILE_FORMAT,
                    CHROME_VERSION
            );
            String intentUrl = intent.getStringExtra("url");
            if (StringUtils.isNotEmpty(intentUrl)) {
                url = intentUrl;
            }
            isEmbeddedSetup = url.startsWith(EMBEDDED_SETUP_URL);
            setContentView(getLayoutIdentifier("revanced_webview"));
            Window window = getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                int visibility = isSDKAbove(26)
                        ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(visibility);
                window.setStatusBarColor(Color.WHITE);
                window.setNavigationBarColor(Color.WHITE);

                if (isSDKAbove(35)) {
                    decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                        v.setBackgroundColor(Color.WHITE);
                        return insets;
                    });
                } else if (isSDKAbove(30)) {
                    WindowInsetsController insetsController = window.getInsetsController();
                    if (insetsController != null) {
                        insetsController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                }
            }
            if (isSDKAbove(33)) {
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                        this::finish
                );
            }
            webView = findViewById(getIdIdentifier("revanced_webview"));

            if (clearCookiesOnStartUp) {
                clearCookies();
            }

            checkWebViewVersion();
            setToolbar();
            loadWebView();

            Logger.printDebug(() -> "onCreate{clearCookiesOnStartUp: " + clearCookiesOnStartUp +
                    ", clearCookiesOnShutDown: " + clearCookiesOnShutDown +
                    ", useDesktopUserAgent: " + useDesktopUserAgent +
                    ", userAgent: " + userAgent +
                    ", url: " + url +
                    "}"
            );
            isInitialized = true;
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate failure", ex);
        }
    }

    @Override
    protected void onDestroy() {
        if (clearCookiesOnShutDown) {
            clearCookies();
        }
        isInitialized = false;
        super.onDestroy();
    }

    protected void clearCookies() {
        if (webView == null) {
            return;
        }
        cookies = "";
        dataSyncId = "";
        visitorData = "";

        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(false);
        webSettings.setGeolocationEnabled(false);
        WebStorage.getInstance().deleteAllData();
        webView.clearHistory();
        webView.clearCache(true);
        webView.clearFormData();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
    }

    protected void setToolbar() {
        ViewGroup toolBarParent = findViewById(getIdIdentifier("revanced_toolbar_parent"));

        // Remove dummy toolbar.
        ViewGroup dummyToolbar = toolBarParent.findViewById(getIdIdentifier("revanced_toolbar"));
        ViewGroup.LayoutParams lp = dummyToolbar.getLayoutParams();
        toolBarParent.removeView(dummyToolbar);

        toolbar = new Toolbar(toolBarParent.getContext());
        toolbar.setBackgroundColor(Color.WHITE);
        toolbar.setNavigationIcon(BaseThemeUtils.getBackButtonDrawable(false));
        toolbar.setNavigationOnClickListener(view -> this.finish());
        toolbar.setTitle(toolbarLabel);
        if (isSDKAbove(24)) {
            final int margin = Utils.dipToPixels(16);
            toolbar.setTitleMarginStart(margin);
            toolbar.setTitleMarginEnd(margin);
        }
        TextView textView = Utils.getChildView(toolbar, view -> view instanceof TextView);
        if (textView != null) {
            textView.setTextColor(Color.BLACK);
        }
        if (BaseThemeUtils.isSupportModernDialog) {
            toolbar.inflateMenu(getMenuIdentifier("revanced_webview_menu"));
            LinearLayout menuParent = Utils.getChildView(toolbar, view -> view instanceof LinearLayout);
            if (menuParent != null) {
                ImageButton menuButton = Utils.getChildView(menuParent, view -> view instanceof ImageButton);
                if (menuButton != null) {
                    menuButton.setImageDrawable(BaseThemeUtils.getMenuButtonDrawable(false));
                }
            }
            int getCookies = getIdIdentifier("revanced_webview_get_cookies");
            int getDataSyncId = getIdIdentifier("revanced_webview_get_data_sync_id");
            int getVisitorData = getIdIdentifier("revanced_webview_get_visitor_data");
            int refresh = getIdIdentifier("revanced_webview_refresh");

            // Set menu item click listener.
            toolbar.setOnMenuItemClickListener(item -> {
                try {
                    int itemId = item.getItemId();
                    if (itemId == getCookies) {
                        showDialog(cookies, str("revanced_webview_cookies"));
                        return true;
                    } else if (itemId == getDataSyncId) {
                        showDialog(dataSyncId, str("revanced_webview_data_sync_id"));
                        return true;
                    } else if (itemId == getVisitorData) {
                        showDialog(visitorData, str("revanced_webview_visitor_data"));
                        return true;
                    } else if (itemId == refresh) {
                        if (webView != null) {
                            webView.reload();
                        }
                        return true;
                    }
                    return false;
                } catch (Exception ex) {
                    Logger.printException(() -> "menu click failure", ex);
                }
                return false;
            });
        }
        toolbar.setLayoutParams(lp);
        toolBarParent.addView(toolbar, 0);
    }

    private void showDialog(String content, String title) {
        if (StringUtils.isEmpty(content)) {
            Utils.showToastShort(str("revanced_webview_toast_empty", title));
            return;
        }
        Context context = webView.getContext();
        if (context == null) {
            return;
        }
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                // Title.
                title,
                // Message.
                content,
                // No EditText.
                null,
                // OK button text.
                str("revanced_webview_copy"),
                // OK button action.
                () -> {
                    Utils.setClipboard(content);
                    Utils.showToastShort(str("revanced_webview_toast_copy_success", title));
                },
                // Cancel button action (dismiss only).
                () -> {
                },
                // Neutral button text.
                null,
                // Neutral button action.
                null,
                // Dismiss dialog when onNeutralClick.
                true
        );
        dialogPair.first.show();
    }

    // Generate a Chrome User-Agent that matches the version of Android System WebView
    protected void checkWebViewVersion() {
        if (!isSDKAbove(26)) {
            return;
        }
        try {
            PackageInfo packageInfo = WebView.getCurrentWebViewPackage();
            if (packageInfo != null) {
                // Android System WebView version name, such as '140.0.7339.51'
                String webViewVersionName = packageInfo.versionName;
                if (StringUtils.isNotEmpty(webViewVersionName)) {
                    String simplifiedVersionName = webViewVersionName;
                    // 140.0.7339.51 -> 140.0.0.0
                    // See 'Post UA-Reduction' in https://www.chromium.org/updates/ua-reduction/#desktop
                    // (Same UA-Reduction applies to all platforms, not just desktop)
                    int dotIndex = webViewVersionName.indexOf(".");
                    if (dotIndex > 0) {
                        String chromiumMajorVersion = webViewVersionName.substring(0, dotIndex);
                        String chromiumVersionSuffix = ".0.0.0";
                        simplifiedVersionName = chromiumMajorVersion + chromiumVersionSuffix;
                    }
                    userAgent = String.format(
                            useDesktopUserAgent
                                    ? USER_AGENT_CHROME_FORMAT
                                    : USER_AGENT_CHROME_MOBILE_FORMAT,
                            simplifiedVersionName
                    );

                    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                    if (applicationInfo != null && applicationInfo.loadLabel(getPackageManager()) instanceof String applicationLabel) {
                        toolbarLabel = applicationLabel + " " + webViewVersionName;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "checkWebViewVersion failed", ex);
        }
    }

    /**
     * Injection point.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    protected boolean containsOAuthTokenIndicator(@NonNull String localCookies) {
        return isEmbeddedSetup && localCookies.contains(OAUTH_TOKEN);
    }

    protected boolean containsVisitorCookieIndicator(@NonNull String localCookies) {
        return localCookies.contains("VISITOR_INFO1_LIVE=");
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void loadWebView() {
        try {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setUserAgentString(userAgent);
            if (isSDKAbove(26)) {
                webSettings.setSafeBrowsingEnabled(true);
            }
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setDomStorageEnabled(true);
            webSettings.setGeolocationEnabled(true);
            webView.setWebViewClient(createWebViewClient());
            if (!isEmbeddedSetup) {
                webView.addJavascriptInterface(new Android(), "Android");
            }
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.acceptThirdPartyCookies(webView);
            cookieManager.setAcceptThirdPartyCookies(webView, true);

            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("Referer", "https://www.google.com/");
            webView.loadUrl(url, extraHeaders);
        } catch (Exception ex) {
            Logger.printException(() -> "loadWebView failed", ex);
        }
    }

    protected WebViewClient createWebViewClient() {
        return new YouTubeWebViewClient();
    }

    private class YouTubeWebViewClient extends WebViewClient {
        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);

            updateCookies();

            if (toolbar != null && view != null) {
                String title = view.getTitle();
                if (StringUtils.isNotEmpty(title)) {
                    toolbar.setTitle(title);
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            updateCookies();
        }

        private void updateCookies() {
            try {
                String localCookies = CookieManager.getInstance().getCookie(url);
                if (localCookies != null && !StringUtils.equals(cookies, localCookies)) {
                    cookies = localCookies;
                    Logger.printDebug(() -> "new Cookies loaded: " + localCookies);

                    if (containsOAuthTokenIndicator(localCookies)) {
                        Map<String, String> cookieMap = AuthUtils.parseCookieString(localCookies);
                        if (!cookieMap.isEmpty()) {
                            String oauth_token = cookieMap.get(OAUTH_TOKEN);
                            webView.evaluateJavascript(
                                    JS_SCRIPT,
                                    email -> {
                                        email = email.replaceAll("\"", "");
                                        YouTubeAuthPatch.setAccessToken(WebViewHostActivity.this, email, oauth_token);
                                    });
                        }
                    } else if (containsVisitorCookieIndicator(localCookies)) {
                        webView.loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)");
                        webView.loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)");
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "updateCookies failed", ex);
            }
        }
    }

    private class Android {
        @JavascriptInterface
        public void onRetrieveVisitorData(@Nullable String newVisitorData) {
            if (newVisitorData != null && !StringUtils.equals(visitorData, newVisitorData)) {
                visitorData = newVisitorData;
                Logger.printDebug(() -> "new Visitor Data loaded: " + newVisitorData);
            }
        }

        @JavascriptInterface
        public void onRetrieveDataSyncId(@Nullable String newDataSyncId) {
            if (newDataSyncId != null && !StringUtils.equals(dataSyncId, newDataSyncId)) {
                dataSyncId = newDataSyncId;
                Logger.printDebug(() -> "new Data Sync Id loaded: " + newDataSyncId);
            }
        }
    }
}
