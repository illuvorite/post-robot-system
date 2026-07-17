package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 操作类型枚举
 * <p>
 * 定义用户在系统中可执行的各种关键操作类型，
 * 用于审计日志记录用户行为轨迹，覆盖登录认证、订单管理、
 * 商品管理、库存调整、任务处理和系统配置等核心业务操作。
 * </p>
 */
@Getter
public enum OperationTypeEnum {

    /** 用户登录系统 */
    LOGIN("LOGIN", "登录"),
    /** 用户登出系统 */
    LOGOUT("LOGOUT", "登出"),
    /** 创建新订单 */
    ORDER_CREATE("ORDER_CREATE", "创建订单"),
    /** 对订单进行支付操作 */
    ORDER_PAY("ORDER_PAY", "订单支付"),
    /** 取消已有订单 */
    ORDER_CANCEL("ORDER_CANCEL", "取消订单"),
    /** 更新商品信息 */
    PRODUCT_UPDATE("PRODUCT_UPDATE", "商品更新"),
    /** 调整库存数量（手动修正） */
    INVENTORY_ADJUST("INVENTORY_ADJUST", "库存调整"),
    /** 人工处理异常任务 */
    TASK_MANUAL("TASK_MANUAL", "人工任务处理"),
    /** 调用邮政系统外部接口 */
    POSTAL_API_CALL("POSTAL_API_CALL", "邮政接口调用"),
    /** 用户权限变更操作 */
    PERMISSION_CHANGE("PERMISSION_CHANGE", "权限变更"),
    /** 导出审计日志 */
    AUDIT_EXPORT("AUDIT_EXPORT", "审计导出"),
    /** 系统配置项变更 */
    CONFIG_CHANGE("CONFIG_CHANGE", "配置变更");

    private final String value;
    private final String text;

    OperationTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static OperationTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OperationTypeEnum type : OperationTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
