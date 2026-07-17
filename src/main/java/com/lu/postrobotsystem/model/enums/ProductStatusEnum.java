package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 商品上下架状态枚举
 * <p>
 * 定义商品的展示状态，控制商品在前端的可见性和可售性。
 * 下架商品不会展示给用户，上架商品可正常浏览和购买。
 * </p>
 */
@Getter
public enum ProductStatusEnum {

    /** 下架：商品不可见、不可售 */
    OFF_SHELF("0", "下架", 0),
    /** 上架：商品正常展示和销售 */
    ON_SHELF("1", "上架", 1);

    private final String value;
    private final String text;
    private final int code;

    ProductStatusEnum(String value, String text, int code) {
        this.value = value;
        this.text = text;
        this.code = code;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static ProductStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (ProductStatusEnum status : ProductStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }

    /**
     * 根据 code 值获取对应的枚举常量
     *
     * @param code 枚举整型编码
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static ProductStatusEnum getEnumByCode(int code) {
        for (ProductStatusEnum status : ProductStatusEnum.values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
