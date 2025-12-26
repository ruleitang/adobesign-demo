package com.example.adobesigndemo.web.dto;

import java.time.Instant;
import java.util.List;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdobeSignWebhookPayload(
        @JsonProperty("event") EventMetadata event,
        @JsonProperty("agreement") AgreementInfo agreement,
        @JsonProperty("participantInfo") ParticipantInfo participantInfo,
        @JsonProperty("documentsInfo") List<DocumentInfo> documentsInfo,
        @JsonProperty("webhook") WebhookInfo webhookInfo) {

    public boolean isAgreementEvent() {
        return event != null && StringUtils.hasText(event.eventType()) && event.eventType().startsWith("AGREEMENT_");
    }

    public String eventType() {
        return event != null ? event.eventType() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventMetadata(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventDate") Instant eventDate,
            @JsonProperty("eventResourceType") String eventResourceType,
            @JsonProperty("eventResourceId") String eventResourceId,
            @JsonProperty("webhookId") String webhookId,
            @JsonProperty("webhookName") String webhookName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgreementInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("status") String status,
            @JsonProperty("message") String message,
            @JsonProperty("expirationTime") Instant expirationTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParticipantInfo(
            @JsonProperty("email") String email,
            @JsonProperty("role") String role,
            @JsonProperty("name") String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("mimeType") String mimeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookInfo(
            @JsonProperty("webhookId") String webhookId,
            @JsonProperty("webhookName") String webhookName,
            @JsonProperty("webhookNotificationId") String webhookNotificationId) {
    }
}
