package com.example.adobesigndemo.service;

import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.adobesigndemo.config.AdobeSignProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class AdobeSignOAuthService {

    private static final long TOKEN_EXPIRY_SAFETY_SECONDS = 30;

    private final AdobeSignProperties properties;
    private final RestClient restClient;
    private volatile String refreshToken;

    private volatile OAuthToken cachedToken;

    public AdobeSignOAuthService(RestClient.Builder builder, AdobeSignProperties properties) {
        this.properties = properties;
        this.restClient = builder.build();
        this.refreshToken = properties.getRefreshToken();
    }

    public String getAccessToken() {
        final OAuthToken token = cachedToken;
        if (token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
            return token.value();
        }
        synchronized (this) {
            final OAuthToken secondCheck = cachedToken;
            if (secondCheck == null || secondCheck.expiresAt().isBefore(Instant.now().plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
                cachedToken = fetchNewToken();
            }
            return cachedToken.value();
        }
    }

    private OAuthToken fetchNewToken() {
        final String tokenToUse = this.refreshToken;
        if (!StringUtils.hasText(tokenToUse)) {
            throw new AdobeSignClientException("No refresh token configured for Adobe Sign.");
        }

        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", tokenToUse);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        final TokenResponse response;
        try {
            response = restClient
                    .post()
                    .uri(properties.getOauthTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientException ex) {
            throw new AdobeSignClientException("Failed to call Adobe Sign OAuth endpoint", ex);
        }

        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new AdobeSignClientException("Failed to retrieve access token from Adobe Sign.");
        }

        if (StringUtils.hasText(response.refreshToken())) {
            this.refreshToken = response.refreshToken();
        }

        final Instant expiresAt = Instant.now().plusSeconds(Math.max(30, response.expiresIn()));
        return new OAuthToken(response.accessToken(), expiresAt);
    }

    private record OAuthToken(String value, Instant expiresAt) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_token") String refreshToken) {
    }
}
