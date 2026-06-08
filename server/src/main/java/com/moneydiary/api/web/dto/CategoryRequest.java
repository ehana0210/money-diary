package com.moneydiary.api.web.dto;

import com.moneydiary.api.domain.TransactionType;

/**
 * 카테고리 생성/수정 요청 본문. {@code type} 은 선택값(null 허용).
 */
public record CategoryRequest(
        String name,
        TransactionType type,
        String icon,
        Boolean custom) {
}
