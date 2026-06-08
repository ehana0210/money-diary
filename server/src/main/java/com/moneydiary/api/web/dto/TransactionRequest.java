package com.moneydiary.api.web.dto;

import com.moneydiary.api.domain.TransactionType;

/**
 * 거래 생성/수정 요청 본문.
 */
public record TransactionRequest(
        TransactionType type,
        Long amount,
        String category,
        String date,
        String memo,
        String clientId) {
}
