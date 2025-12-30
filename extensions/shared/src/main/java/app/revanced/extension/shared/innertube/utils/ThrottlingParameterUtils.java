package app.revanced.extension.shared.innertube.utils;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.youtubeapi.app.playerdata.PlayerDataExtractor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.innertube.client.YouTubeClient;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The functions used in this class are referenced below:
 * - <a href="https://github.com/felipeucelli/JavaTube/blob/ec9011fa2ed584b867d276e683c421059b87bec5/src/main/java/com/github/felipeucelli/javatube/Youtube.java">JavaTube</a>
 * - <a href="https://github.com/TeamNewPipe/NewPipeExtractor/blob/68b4c9acbae2d167e7b1209bb6bf0ae086dd427e/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeJavaScriptExtractor.java">NewPipeExtractor</a>
 */
public class ThrottlingParameterUtils {
    /**
     * Regular expression pattern to find the signature timestamp.
     */
    private static final Pattern SIGNATURE_TIMESTAMP_PATTERN = Pattern.compile("signatureTimestamp[=:](\\d+)");
    /**
     * Regular expression pattern to find the 'n' parameter in streamingUrl.
     */
    private static final Pattern THROTTLING_PARAM_N_PATTERN = Pattern.compile("[&?]n=([^&]+)");
    /**
     * Regular expression pattern to find the 's' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_S_PATTERN = Pattern.compile("s=([^&]+)");
    /**
     * Regular expression pattern to find the 'url' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_URL_PATTERN = Pattern.compile("&url=([^&]+)");
    /**
     * Regular expression pattern to find variables used in JavaScript url.
     */
    private static final Pattern PLAYER_JS_IDENTIFIER_PATTERN =
            Pattern.compile("player/([a-z0-9]{8})/");
    /**
     * Base YouTube Url.
     */
    private static final String BASE_YOUTUBE_URL =
            "https://www.youtube.com";
    /**
     * Client Config url.
     */
    private static final String CLIENT_CONFIG_URL =
            BASE_YOUTUBE_URL + "/tv_config?action_get_config=true";
    /**
     * User-agent.
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 8.0) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/7.0 Chrome/108.0.5359.1 TV Safari/537.36";
    /**
     * Client config Json Object.
     */
    @Nullable
    private volatile static JSONObject clientConfigJsonObject = null;
    /**
     * Client version from client config.
     */
    @Nullable
    private volatile static String clientVersion = null;
    /**
     * Class used to deobfuscate, powered by SmartTube.
     */
    @Nullable
    private volatile static PlayerDataExtractor extractor = null;
    /**
     * Javascript contents.
     */
    @Nullable
    private volatile static String playerJs = null;
    /**
     * Javascript url identifier.
     */
    @Nullable
    private volatile static String playerJsIdentifier = null;
    /**
     * Javascript url.
     */
    @Nullable
    private volatile static String playerJsUrl = null;
    /**
     * Field value included when sending a request.
     */
    @Nullable
    private volatile static Integer signatureTimestamp = null;

    private volatile static boolean isInitialized = false;

    /**
     * Typically, there are 10 to 30 available formats for a video.
     * Each format has a different streaming url, but the 'n' parameter in the response is the same.
     * If the obfuscated 'n' parameter and the deobfuscated 'n' parameter are put in a Map,
     * the remaining 9 to 29 streaming urls can be deobfuscated quickly using the values put in the Map.
     */
    private static final Map<String, String> nParamMap = new LinkedHashMap<>() {
        private static final int NUMBER_OF_N_PARAM = 50;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_N_PARAM;
        }
    };

    public static void initializeJavascript() {
        if (isInitialized) {
            return;
        }
        if (!Utils.isNetworkConnected()) {
            return;
        }
        isInitialized = true;

        extractor = getExtractor();
        playerJs = getPlayerJs();
        playerJsUrl = getPlayerJsUrl();
        signatureTimestamp = getSignatureTimestamp();

        clientVersion = getClientVersion();
        clientConfigJsonObject = getClientConfigJsonObject();
    }

    private static void resetAll() {
        isInitialized = false;

        clientVersion = null;
        extractor = null;
        playerJs = null;
        playerJsUrl = null;
        signatureTimestamp = null;
    }

    private static JSONObject setClientConfigJsonObject() {
        String jsonString = fetch(CLIENT_CONFIG_URL);
        if (jsonString != null) {
            if (jsonString.startsWith(")]}'"))
                jsonString = jsonString.substring(4);

            try {
                JSONObject jsonObject = new JSONObject(jsonString);

                return jsonObject
                        .getJSONObject("webPlayerContextConfig")
                        .getJSONObject("WEB_PLAYER_CONTEXT_CONFIG_ID_LIVING_ROOM_WATCH");
            } catch (Exception ex) {
                Logger.printException(() -> "setClientConfigJsonObject failed", ex);
            }
        }
        return null;
    }

    private static JSONObject getClientConfigJsonObject() {
        if (clientConfigJsonObject == null) {
            clientConfigJsonObject = setClientConfigJsonObject();
        }
        return clientConfigJsonObject;
    }

    @Nullable
    private static Integer setSignatureTimestamp() {
        try {
            String playerJs = getPlayerJs();
            if (playerJs != null) {
                Matcher matcher = SIGNATURE_TIMESTAMP_PATTERN.matcher(playerJs);
                if (matcher.find()) {
                    String signatureTimestamp = matcher.group(1);
                    if (StringUtils.isNotEmpty(signatureTimestamp)) {
                        Logger.printDebug(() -> "signatureTimestamp: " + signatureTimestamp);
                        return Helpers.parseInt(signatureTimestamp);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setSignatureTimestamp failed", ex);
        }
        Logger.printDebug(() -> "signatureTimestamp not found");
        return null;
    }

    @Nullable
    public static Integer getSignatureTimestamp() {
        if (signatureTimestamp == null) {
            signatureTimestamp = setSignatureTimestamp();
        }
        return signatureTimestamp;
    }

    private static String setClientVersion() {
        String clientVersion = null;
        JSONObject clientConfigJsonObject = getClientConfigJsonObject();

        if (clientConfigJsonObject != null) {
            try {
                clientVersion = clientConfigJsonObject
                        .getString("innertubeContextClientVersion");
            } catch (Exception ex) {
                Logger.printException(() -> "setClientVersion failed", ex);
            }
        }

        if (StringUtils.isNotEmpty(clientVersion)) {
            String finalClientVersion = clientVersion;
            Logger.printDebug(() -> "clientVersion: " + finalClientVersion);
            return clientVersion;
        }

        return null;
    }

    public static String getClientVersion() {
        if (clientVersion == null) {
            clientVersion = setClientVersion();
        }

        return clientVersion;
    }

    public static String getClientVersion(YouTubeClient.ClientType clientType) {
        String hardCodedClientVersion = clientType.getClientVersion();
        if (clientType == YouTubeClient.ClientType.TV) {
            if (clientVersion != null) {
                return clientVersion;
            }
        }

        return hardCodedClientVersion;
    }

    @Nullable
    private static String setPlayerJsUrl() {
        JSONObject clientConfigJsonObject = getClientConfigJsonObject();

        if (clientConfigJsonObject != null) {
            try {
                String jsUrl = clientConfigJsonObject
                        .getString("jsUrl");
                Logger.printDebug(() -> "jsUrl: " + jsUrl);
                if (StringUtils.isNotEmpty(jsUrl)) {
                    Matcher matcher = PLAYER_JS_IDENTIFIER_PATTERN.matcher(jsUrl);
                    if (matcher.find()) {
                        playerJsIdentifier = matcher.group(1);
                        return BASE_YOUTUBE_URL + jsUrl;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "setPlayerJsUrl failed", ex);
            }
        }

        Logger.printDebug(() -> "clientConfigJsonObject is null");
        return null;
    }

    @Nullable
    private static String getPlayerJsUrl() {
        if (playerJsUrl == null) {
            playerJsUrl = setPlayerJsUrl();
        }
        return playerJsUrl;
    }

    @Nullable
    private static String setPlayerJs() {
        String playerJsUrl = getPlayerJsUrl();
        if (playerJsUrl != null) {
            return fetch(playerJsUrl);
        }
        return null;
    }

    @Nullable
    private static String getPlayerJs() {
        if (playerJs == null) {
            playerJs = setPlayerJs();
        }
        return playerJs;
    }

    @Nullable
    private static PlayerDataExtractor setExtractor() {
        String playerJs = getPlayerJs();
        if (playerJs != null) {
            return new PlayerDataExtractor(playerJs, Objects.requireNonNull(playerJsIdentifier));
        }
        return null;
    }

    @Nullable
    private static PlayerDataExtractor getExtractor() {
        if (extractor == null) {
            extractor = setExtractor();
        }
        return extractor;
    }

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex) {
        Logger.printDebug(() -> toastMessage, ex);
    }

    @Nullable
    public static String fetch(@NonNull String url) {
        try {
            return Utils.submitOnBackgroundThread(() -> fetchUrl(url)).get();
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printDebug(() -> "Could not fetch url: " + url, ex);
        }

        return null;
    }

    @Nullable
    private static String fetchUrl(@NonNull String uri) {
        Utils.verifyOffMainThread();

        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "fetching url: " + uri);

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .header("Accept-Language", "en-US,en")
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        return responseBody.string();
                    }
                } else {
                    handleConnectionError("API not available with response code: "
                            + response.code() + " message: " + response.message(), null);
                }
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "fetching url failed", ex);
        } finally {
            Logger.printDebug(() -> "fetched url: " + uri + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    @Nullable
    public static String deobfuscateStreamingUrl(
            @NonNull String videoId,
            @Nullable String url,
            @Nullable String signatureCipher) {
        String streamUrl = null;
        if (StringUtils.isNotEmpty(url)) {
            streamUrl = url;
        } else if (StringUtils.isNotEmpty(signatureCipher)) {
            streamUrl = getUrlWithThrottlingParameterObfuscated(
                    videoId,
                    signatureCipher
            );
        }
        if (StringUtils.isNotEmpty(streamUrl)) {
            return getUrlWithThrottlingParameterDeobfuscated(
                    videoId,
                    streamUrl
            );
        }
        return null;
    }

    /**
     * Convert signatureCipher to streaming url with obfuscated 'n' parameter.
     * <p>
     *
     * @param videoId         Current video id.
     * @param signatureCipher The 'signatureCipher' included in the response.
     * @return Streaming url with obfuscated 'n' parameter.
     */
    @Nullable
    private static String getUrlWithThrottlingParameterObfuscated(@NonNull String videoId, @NonNull String signatureCipher) {
        try {
            PlayerDataExtractor extractor = getExtractor();
            if (extractor != null) {
                Matcher paramSMatcher = THROTTLING_PARAM_S_PATTERN.matcher(signatureCipher);
                Matcher paramUrlMatcher = THROTTLING_PARAM_URL_PATTERN.matcher(signatureCipher);
                if (paramSMatcher.find() && paramUrlMatcher.find()) {
                    // The 's' parameter from signatureCipher.
                    String sParam = paramSMatcher.group(1);
                    // The 'url' parameter from signatureCipher.
                    String urlParam = paramUrlMatcher.group(1);
                    if (StringUtils.isNotEmpty(sParam) && StringUtils.isNotEmpty(urlParam)) {
                        // The 'sig' parameter converted by javascript rules.
                        String decodedSigParm = extractor.extractSig(Helpers.decode(sParam));
                        if (StringUtils.isNotEmpty(decodedSigParm)) {
                            String decodedUriParm = Helpers.decode(urlParam);
                            Logger.printDebug(() -> "Converted signatureCipher to obfuscatedUrl, videoId: " + videoId);
                            return decodedUriParm + "&sig=" + decodedSigParm;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterObfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to convert signatureCipher, videoId: " + videoId);
        return null;
    }

    /**
     * Deobfuscates the obfuscated 'n' parameter to a valid streaming url.
     * <p>
     *
     * @param videoId       Current video id.
     * @param obfuscatedUrl Streaming url with obfuscated 'n' parameter.
     * @return Deobfuscated streaming url.
     */
    @Nullable
    private static String getUrlWithThrottlingParameterDeobfuscated(@NonNull String videoId, @NonNull String obfuscatedUrl) {
        try {
            // Obfuscated url is empty.
            if (StringUtils.isEmpty(obfuscatedUrl)) {
                Logger.printDebug(() -> "obfuscatedUrl is empty, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // The 'n' parameter from obfuscatedUrl.
            String obfuscatedNParams = getThrottlingParameterFromStreamingUrl(obfuscatedUrl);

            // The 'n' parameter is null or empty.
            if (StringUtils.isEmpty(obfuscatedNParams)) {
                Logger.printDebug(() -> "'n' parameter not found in obfuscated streaming url, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // If the deobfuscated 'n' parameter is in the Map, return it.
            String deobfuscatedNParam = nParamMap.get(obfuscatedNParams);
            if (deobfuscatedNParam != null) {
                Logger.printDebug(() -> "Cached 'n' parameter found, videoId: " + videoId + ", deobfuscatedNParams: " + deobfuscatedNParam);
                return replaceNParam(obfuscatedUrl, obfuscatedNParams, deobfuscatedNParam);
            }

            // Deobfuscate the 'n' parameter.
            Pair<String, String> deobfuscatedNParamPairs = decodeNParam(obfuscatedUrl, obfuscatedNParams);
            String deobfuscatedUrl = deobfuscatedNParamPairs.first;
            String deobfuscatedNParams = deobfuscatedNParamPairs.second;
            if (!deobfuscatedNParams.isEmpty()) {
                // If the 'n' parameter obfuscation was successful, put it in the map.
                nParamMap.put(obfuscatedNParams, deobfuscatedNParams);
                Logger.printDebug(() -> "Deobfuscated the 'n' parameter, videoId: " + videoId + ", obfuscatedNParams: " + obfuscatedNParams + ", deobfuscatedNParams: " + deobfuscatedNParams);
                return deobfuscatedUrl;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterDeobfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to obfuscate 'n' parameter, videoId: " + videoId);
        return obfuscatedUrl;
    }

    /**
     * Extract the 'n' parameter from the streaming Url.
     * <p>
     *
     * @param streamingUrl The streaming url.
     * @return The 'n' parameter.
     */
    @Nullable
    private static String getThrottlingParameterFromStreamingUrl(@NonNull String streamingUrl) {
        if (streamingUrl.contains("&n=") || streamingUrl.contains("?n=")) {
            final Matcher matcher = THROTTLING_PARAM_N_PATTERN.matcher(streamingUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    /**
     * Replace the 'n' parameter.
     *
     * @param obfuscatedUrl       Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams   Obfuscated 'n' parameter.
     * @param deObfuscatedNParams Deobfuscated 'n' parameter.
     * @return Deobfuscated streaming url.
     */
    @NonNull
    private static String replaceNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams, @NonNull String deObfuscatedNParams) {
        return obfuscatedUrl.replaceFirst("n=" + obfuscatedNParams, "n=" + deObfuscatedNParams);
    }

    /**
     * Deobfuscate the 'n' parameter.
     * <p>
     *
     * @param obfuscatedUrl     Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams Obfuscated 'n' parameter.
     * @return Deobfuscated Pair(Deobfuscated streaming url, Deobfuscated 'n' parameter).
     */
    @NonNull
    private static Pair<String, String> decodeNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams) {
        try {
            PlayerDataExtractor extractor = getExtractor();
            if (extractor != null) {
                // The 'n' parameter deobfuscated by javascript rules.
                String deObfuscatedNParams = extractor.extractNSig(obfuscatedNParams);
                if (StringUtils.isNotEmpty(deObfuscatedNParams)) {
                    String deObfuscatedUrl = replaceNParam(obfuscatedUrl, obfuscatedNParams, deObfuscatedNParams);
                    return new Pair<>(deObfuscatedUrl, deObfuscatedNParams);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "decodeNSig failed", ex);
        }

        return new Pair<>(obfuscatedUrl, "");
    }
}