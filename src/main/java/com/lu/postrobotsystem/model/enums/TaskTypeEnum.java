package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum TaskTypeEnum {
    NAVIGATION("NAVIGATION", "导航"),
    GRASP("GRASP", "抓取"),
    DISPLAY("DISPLAY", "展示"),
    EXPLAIN("EXPLAIN", "讲解"),
    SETTLEMENT("SETTLEMENT", "结算"),
    INVENTORY_CHECK("INVENTORY_CHECK", "库存巡检"),
    PATROL("PATROL", "巡检"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String text;

    TaskTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static TaskTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (TaskTypeEnum type : TaskTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
