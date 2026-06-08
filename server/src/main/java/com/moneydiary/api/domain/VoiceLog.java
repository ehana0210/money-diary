package com.moneydiary.api.domain;

import com.google.cloud.firestore.annotation.Exclude;

/**
 * 음성/자연어 입력 한 건의 처리 기록. {@code users/{uid}/voiceLogs} 에 적재된다.
 *
 * <p>유효/무효(필터)와 파싱 결과를 함께 남겨 사용자 입력 패턴 분석 및 향후 개선에 활용한다.
 */
public class VoiceLog {

    /** 처리 상태. */
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_FILTERED_NON_TX = "FILTERED_NON_TX";
    public static final String STATUS_FILTERED_PROFANITY = "FILTERED_PROFANITY";
    public static final String STATUS_ERROR = "ERROR";

    private String id;

    /** 원문(음성 인식 결과 또는 입력 텍스트). */
    private String rawText;
    /** 입력 출처: "voice" | "text". */
    private String source;
    /** 처리 상태(ACCEPTED / FILTERED_* / ERROR). */
    private String status;
    /** 무효/필터 사유(없으면 빈 문자열). */
    private String reason;
    /** 거래로 유효하게 해석됐는지. */
    private boolean valid;

    // --- 파싱 결과 스냅샷 (유효할 때 채워짐) ---
    private String type;
    private long amount;
    private String categoryId;
    private String categoryName;
    private String date;
    private String memo;

    /** 생성 시각 (epoch millis). */
    private long createdAt;

    public VoiceLog() {
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
