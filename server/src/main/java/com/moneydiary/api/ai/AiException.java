package com.moneydiary.api.ai;

/**
 * LLM 호출/응답 처리 중 발생한 오류(업스트림 502 로 매핑).
 */
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
