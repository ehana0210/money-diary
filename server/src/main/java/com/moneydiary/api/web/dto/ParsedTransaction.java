package com.moneydiary.api.web.dto;

import com.moneydiary.api.domain.TransactionType;

/**
 * LLM 이 자연어를 해석해 만든 거래 후보. 실제 저장은 클라이언트가 확인 후
 * {@code POST /api/transactions} 로 진행한다(서버는 여기서 저장하지 않는다).
 *
 * @param valid        거래로 유효하게 해석됐는지. false 면 폼을 채우지 않고 안내만 한다
 * @param reason       무효/필터 사유(유효하면 빈 문자열)
 * @param type         INCOME / EXPENSE
 * @param amount       금액(원)
 * @param category     매칭된 기존 카테고리 id. 없으면 null
 * @param categoryName LLM 이 고른 카테고리 이름(매칭 실패 시 신규 후보)
 * @param matched      categoryName 이 기존 카테고리와 매칭됐는지 여부
 * @param date         yyyy-MM-dd
 * @param memo         메모(없으면 빈 문자열)
 */
public record ParsedTransaction(
        boolean valid,
        String reason,
        TransactionType type,
        long amount,
        String category,
        String categoryName,
        boolean matched,
        String date,
        String memo) {

    /** 거래로 해석되지 않은(필터된) 결과. */
    public static ParsedTransaction invalid(String reason) {
        return new ParsedTransaction(false, reason, null, 0, null, null, false, null, "");
    }
}
