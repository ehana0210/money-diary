package com.moneydiary.api.web.dto;

/**
 * 자연어 한 줄 입력. 예: "어제 떡볶이 3000원".
 *
 * @param text   입력 원문
 * @param source 입력 출처: "voice" | "text" (없으면 "voice" 로 간주)
 */
public record ParseTransactionRequest(String text, String source) {
}
