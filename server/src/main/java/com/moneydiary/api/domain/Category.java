package com.moneydiary.api.domain;

import com.google.cloud.firestore.annotation.Exclude;

/**
 * 사용자가 정의하는 카테고리.
 *
 * <p>{@code type} 이 비어 있으면 들어온 돈/나간 돈 양쪽에서 모두 쓰는 카테고리로 간주한다.
 */
public class Category {

    private String id;
    private String name;
    private TransactionType type;
    /** 카테고리 아이콘(이모지). */
    private String icon;
    /** 사용자가 추가한 카테고리면 true, 기본 제공 카테고리면 false. */
    private boolean custom;
    /** 생성 시각 (epoch millis). */
    private long createdAt;

    public Category() {
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
