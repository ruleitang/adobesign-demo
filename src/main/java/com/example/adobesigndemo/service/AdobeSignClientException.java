package com.example.adobesigndemo.service;

public class AdobeSignClientException extends RuntimeException {

    public AdobeSignClientException(String message) {
        super(message);
    }

    public AdobeSignClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
