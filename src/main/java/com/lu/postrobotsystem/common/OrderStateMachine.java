package com.lu.postrobotsystem.common;

import com.lu.postrobotsystem.exception.BusinessException;
import com.lu.postrobotsystem.model.enums.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态机
 * <p>
 * 定义订单状态的合法转换规则（状态转换矩阵），禁止非法跳转。
 * 采用 {@link EnumMap} 存储每个状态允许到达的下一状态集合，
 * 提供 {@link #transition(OrderStatusEnum, OrderStatusEnum)} 方法校验状态转换合法性。
 * </p>
 *
 * <p><b>状态转换矩阵（有向图）：</b><br>
 * <pre>
 * PENDING_PAY ───→ PAYING      （用户发起支付）
 * PENDING_PAY ───→ CANCELLED   （用户取消）
 * PENDING_PAY ───→ TIMEOUT     （支付超时）
 *      │
 * PAYING ─────────→ PAID              （支付成功）
 * PAYING ─────────→ FAILED            （支付失败）
 * PAYING ─────────→ TIMEOUT           （支付超时）
 * PAYING ─────────→ MANUAL_REQUIRED   （需要人工介入）
 *      │
 * FAILED ─────────→ PAYING            （重试支付）
 * FAILED ─────────→ CANCELLED         （用户放弃）
 * FAILED ─────────→ TIMEOUT           （超时未处理）
 * FAILED ─────────→ MANUAL_REQUIRED   （需人工介入）
 *      │
 * TIMEOUT ────────→ PENDING_PAY       （重新发起支付）
 * TIMEOUT ────────→ MANUAL_REQUIRED   （需人工介入）
 *      │
 * MANUAL_REQUIRED ─→ PAYING           （人工介入后重试支付）
 * MANUAL_REQUIRED ─→ PAID             （人工确认支付成功）
 * MANUAL_REQUIRED ─→ FAILED           （人工确认支付失败）
 * MANUAL_REQUIRED ─→ CANCELLED        （人工取消）
 *      │
 * PAID ───────────→ （终态，无合法出口）
 * CANCELLED ──────→ （终态，无合法出口）
 * </pre>
 * </p>
 */
@Slf4j
public final class OrderStateMachine {

    /** 状态转换矩阵：key=当前状态，value=允许的下一状态集合 */
    private static final EnumMap<OrderStatusEnum, Set<OrderStatusEnum>> TRANSITION_MAP = new EnumMap<>(OrderStatusEnum.class);

    // 静态初始化块：填充状态转换矩阵
    static {
        // PENDING_PAY → PAYING（发起支付）/ CANCELLED（取消）/ TIMEOUT（超时）
        TRANSITION_MAP.put(OrderStatusEnum.PENDING_PAY, EnumSet.of(
                OrderStatusEnum.PAYING,
                OrderStatusEnum.CANCELLED,
                OrderStatusEnum.TIMEOUT
        ));

        // PAYING → PAID（成功）/ FAILED（失败）/ TIMEOUT（超时）/ MANUAL_REQUIRED（人工介入）
        TRANSITION_MAP.put(OrderStatusEnum.PAYING, EnumSet.of(
                OrderStatusEnum.PAID,
                OrderStatusEnum.FAILED,
                OrderStatusEnum.TIMEOUT,
                OrderStatusEnum.MANUAL_REQUIRED
        ));

        // FAILED → PAYING（重试）/ CANCELLED（放弃）/ TIMEOUT（超时）/ MANUAL_REQUIRED（人工介入）
        TRANSITION_MAP.put(OrderStatusEnum.FAILED, EnumSet.of(
                OrderStatusEnum.PAYING,
                OrderStatusEnum.CANCELLED,
                OrderStatusEnum.TIMEOUT,
                OrderStatusEnum.MANUAL_REQUIRED
        ));

        // TIMEOUT → PENDING_PAY（重新支付）/ MANUAL_REQUIRED（人工介入）
        TRANSITION_MAP.put(OrderStatusEnum.TIMEOUT, EnumSet.of(
                OrderStatusEnum.PENDING_PAY,
                OrderStatusEnum.MANUAL_REQUIRED
        ));

        // MANUAL_REQUIRED → PAYING（重试）/ PAID（确认成功）/ FAILED（确认失败）/ CANCELLED（取消）
        TRANSITION_MAP.put(OrderStatusEnum.MANUAL_REQUIRED, EnumSet.of(
                OrderStatusEnum.PAYING,
                OrderStatusEnum.PAID,
                OrderStatusEnum.FAILED,
                OrderStatusEnum.CANCELLED
        ));

        // PAID / CANCELLED 是终态，不定义出口，任何离开这些状态的转换都会被拒绝
    }

    /** 终态集合 */
    private static final Set<OrderStatusEnum> TERMINAL_STATES = EnumSet.of(
            OrderStatusEnum.PAID,
            OrderStatusEnum.CANCELLED
    );

    private OrderStateMachine() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 校验订单状态转换是否合法
     * <p>
     * 根据状态转换矩阵判断从当前状态到目标状态的转换是否被允许。
     * 若转换非法，抛出 {@link BusinessException}，并记录告警日志。
     * </p>
     *
     * // 1. 检查当前状态是否为终态（PAID / CANCELLED 不可再变）
     * // 2. 检查转换矩阵中是否包含目标状态
     * // 3. 非法转换时抛 BusinessException 并记录告警
     *
     * @param current 当前状态
     * @param target  目标状态
     * @throws BusinessException 若转换非法，携带明确的错误描述
     */
    public static void transition(OrderStatusEnum current, OrderStatusEnum target) {
        // 1. 检查当前状态是否是终态（终态不允许任何转换）
        if (TERMINAL_STATES.contains(current)) {
            log.warn("非法状态转换: 终态[{}]不可变更为[{}]", current, target);
            throw new BusinessException("订单状态非法转换: " + current.getText() + " 不允许变更为 " + target.getText());
        }

        // 2. 查询转换矩阵获取允许的目标状态集合
        Set<OrderStatusEnum> allowedTargets = TRANSITION_MAP.get(current);

        // 3. 若当前状态无定义转换规则，或目标状态不在允许集合中，则拒绝
        if (allowedTargets == null || !allowedTargets.contains(target)) {
            log.warn("非法状态转换: [{}] → [{}] 不被允许", current, target);
            throw new BusinessException("订单状态非法转换: " + current.getText() + " 不允许变更为 " + target.getText());
        }
    }

    /**
     * 判断订单状态是否为终态
     *
     * @param status 待判断的订单状态
     * @return true=终态（不可再变更），false=非终态（可继续流转）
     */
    public static boolean isTerminal(OrderStatusEnum status) {
        return TERMINAL_STATES.contains(status);
    }

    /**
     * 判断两个状态之间的转换是否合法（不抛异常版本）
     *
     * @param current 当前状态
     * @param target  目标状态
     * @return true=转换合法，false=转换非法
     */
    public static boolean isAllowed(OrderStatusEnum current, OrderStatusEnum target) {
        if (TERMINAL_STATES.contains(current)) return false;
        Set<OrderStatusEnum> allowedTargets = TRANSITION_MAP.get(current);
        return allowedTargets != null && allowedTargets.contains(target);
    }
}
