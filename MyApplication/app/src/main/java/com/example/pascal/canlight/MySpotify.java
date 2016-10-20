package com.example.pascal.canlight;

import android.app.Activity;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import junit.framework.AssertionFailedError;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by pascal on 09.10.16.
 */
public class MySpotify {
    public static String CLIENT_ID;
    public static final String REDIRECT_URL = "canlight-spotify://callback";
    private static SpotifyApi spotifyApi = null;

    public static SpotifyApi getSpotifyApi() {
        if (spotifyApi == null) {
            spotifyApi = new SpotifyApi();
        }
        return spotifyApi;
    }

    public static SpotifyService getSpotifyService() {
        return getSpotifyApi().getService();
    }

    public static void loginRequest(Activity activity) {
        CLIENT_ID = activity.getString(R.string.spotify_client_id);
        // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(MySpotify.CLIENT_ID,
                        AuthenticationResponse.Type.TOKEN,
                        MySpotify.REDIRECT_URL);

        builder.setScopes(new String[] { "streaming" });
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(activity,
                MainActivity.LOGIN_SPOTIFY_REQUEST,
                request);

        spotifyApi = new SpotifyApi();
    }

    public static AuthenticationResponse.Type onLoginResponse(AuthenticationResponse r) {
        switch (r.getType()) {
            // Response was successful and contains auth token
            case TOKEN:
                // Handle successful response
                spotifyApi.setAccessToken(r.getAccessToken());
                break;
            default:
                spotifyApi = null;
                break;
        }
        return r.getType();
    }
}
