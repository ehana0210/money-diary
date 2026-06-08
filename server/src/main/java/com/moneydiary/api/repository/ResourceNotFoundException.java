package com.moneydiary.api.repository;

/**
 * 요청한 문서가 존재하지 않을 때 던진다. (HTTP 404 로 변환)
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
