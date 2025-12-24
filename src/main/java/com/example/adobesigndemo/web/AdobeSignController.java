package com.example.adobesigndemo.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.adobesigndemo.service.AdobeSignService;
import com.example.adobesigndemo.web.dto.SendAgreementRequest;
import com.example.adobesigndemo.web.dto.SendAgreementResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agreements")
public class AdobeSignController {

    private final AdobeSignService adobeSignService;

    public AdobeSignController(AdobeSignService adobeSignService) {
        this.adobeSignService = adobeSignService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SendAgreementResponse sendAgreement(@Valid @RequestBody SendAgreementRequest request) {
        return adobeSignService.sendAgreement(request);
    }
}
