package com.example.adobesigndemo.web.dto;

import java.time.Instant;

public record SendAgreementResponse(
        String agreementId,
        Instant expiresAt,
        String senderViewUrl,
        String signingUrl
) {
}
