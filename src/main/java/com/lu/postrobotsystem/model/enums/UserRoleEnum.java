package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    ADMIN("ADMIN", "管理员"),
    OPERATOR("OPERATOR", "运营人员"),
    MAINTAINER("MAINTAINER", "维护人员");

    private final String value;
    private final String text;

    UserRoleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.value.equals(value)) return role;
        }
        return null;
    }
}
