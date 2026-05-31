package com.swp.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swp.util.GoogleConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GoogleOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String buildAuthorizationUrl(String state, String redirectUri) {
        String query = "client_id=" + encode(GoogleConfig.getClientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&access_type=online"
                + "&prompt=select_account"
                + "&state=" + encode(state);
        return AUTH_URL + "?" + query;
    }

    public GoogleUserInfo fetchUserInfo(String authorizationCode, String redirectUri)
            throws IOException, InterruptedException {
        String accessToken = exchangeCodeForAccessToken(authorizationCode, redirectUri);
        return fetchUserInfoWithToken(accessToken);
    }

    private String exchangeCodeForAccessToken(String code, String redirectUri)
            throws IOException, InterruptedException {
        String body = "code=" + encode(code)
                + "&client_id=" + encode(GoogleConfig.getClientId())
                + "&client_secret=" + encode(GoogleConfig.getClientSecret())
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Google token error: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("access_token")) {
            throw new IOException("Không nhận được access_token từ Google.");
        }
        return json.get("access_token").getAsString();
    }

    private GoogleUserInfo fetchUserInfoWithToken(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Google userinfo error: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String googleId = json.has("sub") ? json.get("sub").getAsString() : null;
        String email = json.has("email") ? json.get("email").getAsString() : null;
        String name = json.has("name") ? json.get("name").getAsString() : "Người dùng Google";
        String picture = json.has("picture") ? json.get("picture").getAsString() : null;

        if (googleId == null || googleId.isBlank() || email == null || email.isBlank()) {
            throw new IOException("Google không trả về đủ thông tin tài khoản.");
        }

        return new GoogleUserInfo(googleId, email, name, picture);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record GoogleUserInfo(String googleId, String email, String fullName, String avatarUrl) {
    }
}
