package com.moneydiary.api.domain;

import com.google.cloud.firestore.annotation.Exclude;

/**
 * 거래 내역(들어온 돈/나간 돈) 한 건.
 *
 * <p>Firestore POJO 매핑을 위해 no-arg 생성자와 getter/setter 를 갖춘 일반 클래스로
 * 작성한다. {@code id} 는 문서 ID 로만 쓰이며 문서 본문에는 저장하지 않는다
 * ({@link Exclude}).
 */
public class Transaction {

    private String id;
    private TransactionType type;
    private long amount;
    private String category;
    /** 거래 날짜 (ISO 형식 yyyy-MM-dd). */
    private String date;
    private String memo;
    /**
     * 클라이언트가 부여한 멱등 키. localStorage 마이그레이션 시 중복 업로드를 방지하는 용도로 쓰며,
     * 일반 생성에서는 비어 있을 수 있다.
     */
    private String clientId;
    /** 생성 시각 (epoch millis). */
    private long createdAt;

    public Transaction() {
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
