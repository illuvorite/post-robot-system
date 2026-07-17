package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 * <p>
 * 定义系统用户的不同角色类型，每个角色对应不同的权限范围。
 * 管理员拥有系统最高权限，运营人员负责日常业务操作，
 * 维护人员负责系统和技术层面的维护工作。
 * </p>
 */
@Getter
public enum UserRoleEnum {

    /** 管理员：拥有系统全部权限，可进行用户管理、配置管理和审计等操作 */
    ADMIN("ADMIN", "管理员"),
    /** 运营人员：负责日常业务运营，包括商品管理、订单处理和告警处理等 */
    OPERATOR("OPERATOR", "运营人员"),
    /** 维护人员：负责系统维护、设备巡检和技术支持等工作 */
    MAINTAINER("MAINTAINER", "维护人员");

    private final String value;
    private final String text;

    UserRoleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.value.equals(value)) return role;
        }
        return null;
    }
}
