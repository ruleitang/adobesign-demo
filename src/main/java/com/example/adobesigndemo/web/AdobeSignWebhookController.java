package com.example.adobesigndemo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.adobesigndemo.service.AdobeSignWebhookService;

@RestController
@RequestMapping("/api/webhooks/adobesign")
public class AdobeSignWebhookController {

    private final AdobeSignWebhookService webhookService;

    public AdobeSignWebhookController(AdobeSignWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhookPost(
            @RequestHeader(value = AdobeSignWebhookService.HEADER_CLIENT_ID, required = false) String clientId,
            @RequestHeader(value = AdobeSignWebhookService.HEADER_SIGNATURE, required = false) String signature,
            @RequestBody(required = false) String payload) {
        webhookService.handleAgreementWebhook(clientId, signature, payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Void> handleWebhookGet(
            @RequestHeader(value = AdobeSignWebhookService.HEADER_CLIENT_ID, required = false) String clientId,
            @RequestHeader(value = AdobeSignWebhookService.HEADER_SIGNATURE, required = false) String signature) {
        webhookService.handleAgreementWebhook(clientId, signature, null);
        return ResponseEntity.ok().build();
    }
}
