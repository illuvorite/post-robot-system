package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum OperationTypeEnum {
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    ORDER_CREATE("ORDER_CREATE", "创建订单"),
    ORDER_PAY("ORDER_PAY", "订单支付"),
    ORDER_CANCEL("ORDER_CANCEL", "取消订单"),
    PRODUCT_UPDATE("PRODUCT_UPDATE", "商品更新"),
    INVENTORY_ADJUST("INVENTORY_ADJUST", "库存调整"),
    TASK_MANUAL("TASK_MANUAL", "人工任务处理"),
    POSTAL_API_CALL("POSTAL_API_CALL", "邮政接口调用"),
    PERMISSION_CHANGE("PERMISSION_CHANGE", "权限变更"),
    AUDIT_EXPORT("AUDIT_EXPORT", "审计导出"),
    CONFIG_CHANGE("CONFIG_CHANGE", "配置变更");

    private final String value;
    private final String text;

    OperationTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static OperationTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OperationTypeEnum type : OperationTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
