package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum ProductStatusEnum {
    OFF_SHELF("0", "下架", 0),
    ON_SHELF("1", "上架", 1);

    private final String value;
    private final String text;
    private final int code;

    ProductStatusEnum(String value, String text, int code) {
        this.value = value;
        this.text = text;
        this.code = code;
    }

    public static ProductStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (ProductStatusEnum status : ProductStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }

    public static ProductStatusEnum getEnumByCode(int code) {
        for (ProductStatusEnum status : ProductStatusEnum.values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
