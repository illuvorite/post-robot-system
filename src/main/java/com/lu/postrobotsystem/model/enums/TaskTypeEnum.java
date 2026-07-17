package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 任务类型枚举
 * <p>
 * 定义机器人可执行的各种任务类型，
 * 覆盖导航移动、商品操作、信息服务和巡检管理等核心业务场景，
 * 用于任务识别、调度和执行逻辑分发。
 * </p>
 */
@Getter
public enum TaskTypeEnum {

    /** 导航任务：机器人在指定区域内移动至目标位置 */
    NAVIGATION("NAVIGATION", "导航"),
    /** 抓取任务：机器人从货架抓取指定商品 */
    GRASP("GRASP", "抓取"),
    /** 展示任务：机器人向用户展示商品信息 */
    DISPLAY("DISPLAY", "展示"),
    /** 讲解任务：机器人对商品进行语音或屏幕讲解 */
    EXPLAIN("EXPLAIN", "讲解"),
    /** 结算任务：机器人辅助完成购物结算流程 */
    SETTLEMENT("SETTLEMENT", "结算"),
    /** 库存巡检任务：机器人对货架进行库存盘点 */
    INVENTORY_CHECK("INVENTORY_CHECK", "库存巡检"),
    /** 巡检任务：机器人执行常规巡逻检查 */
    PATROL("PATROL", "巡检"),
    /** 其他类型：未归类的自定义任务 */
    OTHER("OTHER", "其他");

    private final String value;
    private final String text;

    TaskTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static TaskTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (TaskTypeEnum type : TaskTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
