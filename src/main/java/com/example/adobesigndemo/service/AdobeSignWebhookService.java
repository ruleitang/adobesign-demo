package com.example.adobesigndemo.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.adobesigndemo.config.AdobeSignProperties;
import com.example.adobesigndemo.web.dto.AdobeSignWebhookPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdobeSignWebhookService {

    public static final String HEADER_CLIENT_ID = "X-AdobeSign-ClientId";
    public static final String HEADER_SIGNATURE = "X-AdobeSign-Signature";

    private static final Logger logger = LoggerFactory.getLogger(AdobeSignWebhookService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AdobeSignProperties properties;
    private final ObjectMapper objectMapper;

    public AdobeSignWebhookService(AdobeSignProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void handleAgreementWebhook(String clientIdHeader, String signatureHeader, String payload) {
        validateClientId(clientIdHeader);

        if (!StringUtils.hasText(payload)) {
            logger.info("Received Adobe Sign webhook handshake for integration {}", properties.getClientId());
            return;
        }

        validateSignature(signatureHeader, payload);

        final AdobeSignWebhookPayload webhookPayload = parsePayload(payload);
        if (!webhookPayload.isAgreementEvent()) {
            logger.debug("Ignoring Adobe Sign event {} because it is not an agreement event", webhookPayload.eventType());
            return;
        }

        logAgreementEvent(webhookPayload);
    }

    private void validateClientId(String clientIdHeader) {
        if (!StringUtils.hasText(clientIdHeader)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER_CLIENT_ID + " header is required");
        }
        if (!clientIdHeader.equals(properties.getClientId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Adobe Sign client id");
        }
    }

    private void validateSignature(String signatureHeader, String payload) {
        final String signingSecret = properties.getWebhookSigningSecret();
        if (!StringUtils.hasText(signingSecret)) {
            logger.warn("Skipping Adobe Sign webhook signature validation because no signing secret is configured");
            return;
        }

        if (!StringUtils.hasText(signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, HEADER_SIGNATURE + " header is required");
        }

        final byte[] expected = hmacSha256(payload, signingSecret);
        final byte[] provided = decodeSignature(signatureHeader);

        if (!MessageDigest.isEqual(expected, provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Adobe Sign webhook signature verification failed");
        }
    }

    private byte[] hmacSha256(String payload, String signingSecret) {
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to initialize HmacSHA256", ex);
        }
    }

    private byte[] decodeSignature(String signatureHeader) {
        String sanitized = signatureHeader.trim();
        if (sanitized.toLowerCase(Locale.ROOT).startsWith("sha256=")) {
            sanitized = sanitized.substring(7);
        }

        sanitized = sanitized.replace(" ", "");

        try {
            return HexFormat.of().parseHex(sanitized);
        } catch (IllegalArgumentException ignored) {
            // not hex encoded
        }

        try {
            return Base64.getDecoder().decode(sanitized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Adobe Sign webhook signature encoding");
        }
    }

    private AdobeSignWebhookPayload parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, AdobeSignWebhookPayload.class);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse Adobe Sign webhook payload", ex);
        }
    }

    private void logAgreementEvent(AdobeSignWebhookPayload payload) {
        final AdobeSignWebhookPayload.EventMetadata event = payload.event();
        final AdobeSignWebhookPayload.AgreementInfo agreement = payload.agreement();
        final AdobeSignWebhookPayload.ParticipantInfo participant = payload.participantInfo();

        logger.info(
                "Adobe Sign event {} for agreement {} (name: {}, status: {}, participant: {})",
                event != null ? event.eventType() : "UNKNOWN",
                agreement != null ? agreement.id() : "UNKNOWN",
                agreement != null ? agreement.name() : "n/a",
                agreement != null ? agreement.status() : "n/a",
                participant != null ? participant.email() : "n/a");
    }
}
