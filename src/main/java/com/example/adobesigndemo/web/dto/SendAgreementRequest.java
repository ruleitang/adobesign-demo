package com.example.adobesigndemo.web.dto;

import java.util.List;

import jakarta.validation.constraints.Email;

public record SendAgreementRequest(
        @Email String recipientEmail,
        List<@Email String> recipientEmails,
        String agreementName,
        String message,
        String documentPath
) {
}
