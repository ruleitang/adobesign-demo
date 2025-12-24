package com.example.adobesigndemo.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "adobesign")
public class AdobeSignProperties {

    @NotNull
    private URI baseUri;

    @NotBlank
    private String apiUserEmail;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotNull
    private URI oauthTokenUri = URI.create("https://secure.na1.adobesign.us/api/gateway/adobesignauthservice/api/v1/token");

    @NotBlank
    private String refreshToken;

    private List<String> defaultRecipientEmails = new ArrayList<>();

    private String defaultAgreementName = "Adobe Sign Test Agreement";

    private String testDocumentPath = "classpath:documents/test-signature.docx";

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public String getApiUserEmail() {
        return apiUserEmail;
    }

    public void setApiUserEmail(String apiUserEmail) {
        this.apiUserEmail = apiUserEmail;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<String> getDefaultRecipientEmails() {
        return defaultRecipientEmails;
    }

    public void setDefaultRecipientEmails(List<String> defaultRecipientEmails) {
        this.defaultRecipientEmails = defaultRecipientEmails;
    }

    public String getDefaultAgreementName() {
        return defaultAgreementName;
    }

    public void setDefaultAgreementName(String defaultAgreementName) {
        this.defaultAgreementName = defaultAgreementName;
    }

    public String getTestDocumentPath() {
        return testDocumentPath;
    }

    public void setTestDocumentPath(String testDocumentPath) {
        this.testDocumentPath = testDocumentPath;
    }

    public URI getOauthTokenUri() {
        return oauthTokenUri;
    }

    public void setOauthTokenUri(URI oauthTokenUri) {
        this.oauthTokenUri = oauthTokenUri;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
