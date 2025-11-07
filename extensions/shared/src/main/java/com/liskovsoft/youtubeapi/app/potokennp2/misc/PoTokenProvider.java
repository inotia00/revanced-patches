package com.liskovsoft.youtubeapi.app.potokennp2.misc;

import androidx.annotation.Nullable;

/**
 * Interface to provide {@code poToken}s to YouTube player requests.
 *
 * <p>
 * On some major clients, YouTube requires that the integrity of the device passes some checks to
 * allow playback.
 * </p>
 *
 * <p>
 * These checks involve running codes to verify the integrity and using their result to generate
 * one or multiple {@code poToken}(s) (which stands for proof of origin token(s)).
 * </p>
 *
 * <p>
 * These tokens may have a role in triggering the sign in requirement.
 * </p>
 *
 * <p>
 * If an implementation does not want to return a {@code poToken} for a specific client, it <b>must
 * return {@code null}</b>.
 * </p>
 *
 * <p>
 * <b>Implementations of this interface are expected to be thread-safe, as they may be accessed by
 * multiple threads.</b>
 * </p>
 */
public interface PoTokenProvider {

    /**
     * Get a {@link PoTokenResult} specific to the desktop website, a.k.a. the WEB InnerTube client.
     *
     * <p>
     * To be generated and valid, {@code poToken}s from this client must be generated using Google's
     * BotGuard machine, which requires a JavaScript engine with a good DOM implementation. They
     * must be added to adaptive/DASH streaming URLs with the {@code pot} parameter.
     * </p>
     *
     * <p>
     * Note that YouTube desktop website generates two {@code poToken}s:
     * - one for the player requests {@code poToken}s, using the videoId as the minter value;
     * - one for the streaming URLs, using a visitor data for logged-out users as the minter value.
     * </p>
     *
     * @return a {@link PoTokenResult} specific to the WEB InnerTube client
     */
    @Nullable
    PoTokenResult getWebClientPoToken(String videoId);

    /**
     * Get a {@link PoTokenResult} specific to the web embeds, a.k.a. the WEB_EMBEDDED_PLAYER
     * InnerTube client.
     *
     * <p>
     * To be generated and valid, {@code poToken}s from this client must be generated using Google's
     * BotGuard machine, which requires a JavaScript engine with a good DOM implementation. They
     * should be added to adaptive/DASH streaming URLs with the {@code pot} parameter.
     * </p>
     *
     * <p>
     * As of writing, like the YouTube desktop website previously did, it generates only one
     * {@code poToken}, sent in player requests and streaming URLs, using a visitor data for
     * logged-out users. {@code poToken}s do not seem to be mandatory for now on this client.
     * </p>
     *
     * @return a {@link PoTokenResult} specific to the WEB_EMBEDDED_PLAYER InnerTube client
     */
    @Nullable
    PoTokenResult getWebEmbedClientPoToken(String videoId);

    /**
     * Get a {@link PoTokenResult} specific to the Android app, a.k.a. the ANDROID InnerTube client.
     *
     * <p>
     * Implementation details are not known, the app uses DroidGuard, a downloaded native virtual
     * machine ran by Google Play Services for which its code is updated pretty frequently.
     * </p>
     *
     * <p>
     * As of writing, DroidGuard seem to check for the Android app signature and package ID, as
     * non-rooted YouTube patched with reVanced doesn't work without spoofing another InnerTube
     * client while the rooted version works without any client spoofing.
     * </p>
     *
     * <p>
     * There should be only one {@code poToken} needed for the player requests, it shouldn't be
     * required for regular adaptive URLs (i.e. not server adaptive bitrate (SABR) URLs). HLS
     * formats returned (only for premieres and running and post-live livestreams) in the client's
     * HLS manifest URL should work without {@code poToken}s.
     * </p>
     *
     * @return a {@link PoTokenResult} specific to the ANDROID InnerTube client
     */
    @Nullable
    PoTokenResult getAndroidClientPoToken(String videoId);

    /**
     * Get a {@link PoTokenResult} specific to the iOS app, a.k.a. the IOS InnerTube client.
     *
     * <p>
     * Implementation details are not known, the app seem to use something called iosGuard which
     * should be similar to Android's DroidGuard. It may rely on Apple's attestation APIs.
     * </p>
     *
     * <p>
     * As of writing, there should be only one {@code poToken} needed for the player requests, it
     * shouldn't be required for regular adaptive URLs (i.e. not server adaptive bitrate (SABR)
     * URLs). HLS formats returned in the client's HLS manifest URL should also work without a
     * {@code poToken}.
     * </p>
     *
     * @return a {@link PoTokenResult} specific to the IOS InnerTube client
     */
    @Nullable
    PoTokenResult getIosClientPoToken(String videoId);

    boolean isWebPotExpired();

    boolean isWebPotSupported();
}
