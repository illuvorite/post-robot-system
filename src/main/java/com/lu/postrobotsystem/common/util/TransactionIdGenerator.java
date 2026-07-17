package com.lu.postrobotsystem.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务ID生成器
 * <p>
 * 用于生成全局唯一的事务标识（TransactionID），
 * 作为与邮政外部系统通信时的请求追踪凭证。
 * 生成的ID格式为：{@code 5位平台编码 + 17位时间戳 + 10位流水号 = 32位}。
 * </p>
 *
 * <p><b>ID结构说明：</b>
 * <ul>
 *   <li><b>平台编码（5位）：</b>固定为 "POSTR"，标识邮政机器人系统</li>
 *   <li><b>时间戳（17位）：</b>格式为 yyyyMMddHHmmssSSS，精确到毫秒</li>
 *   <li><b>流水号（10位）：</b>基于 {@link AtomicLong} 的递增序列，从 0000000000 开始，达到 9999999999 后归零</li>
 * </ul>
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>被 {@link SignUtil#sign} 调用，作为签名计算参数之一</li>
 *   <li>被发送请求的拦截器或客户端调用，设置请求头中的 TransactionID</li>
 * </ul>
 * </p>
 *
 * <p><b>线程安全：</b>
 * 使用 {@link AtomicLong} 保证高并发场景下流水号的线程安全性。
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
public class TransactionIdGenerator {

    /** 平台编码：POSTR = POST Robot System（邮政机器人系统） */
    private static final String PLATFORM_CODE = "POSTR";

    /** 日期时间格式化器，格式精确到毫秒：yyyyMMddHHmmssSSS */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 原子递增的序列号生成器，保证线程安全的流水号递增 */
    private static final AtomicLong SEQ = new AtomicLong(0);

    /**
     * 生成全局唯一的事务ID
     * <p>
     * ID格式：{@code POSTR + yyyyMMddHHmmssSSS + 0000000000} = 共32位。
     * 当序列号达到 9999999999（即100亿-1）时自动归零重新计数。
     * </p>
     *
     * @return 32位全局唯一事务ID字符串
     * @see SignUtil#sign 该ID被签名计算所使用
     */
    public static String generate() {
        // 原子自增获取下一个序列号
        long seq = SEQ.incrementAndGet();
        // 当序列号超过 9999999999 时重置为 0
        if (seq > 9999999999L) {
            SEQ.set(0);
            seq = 0;
        }
        // 拼接 平台编码 + 时间戳 + 10位补零流水号
        return PLATFORM_CODE
             + LocalDateTime.now().format(FORMATTER)
             + String.format("%010d", seq);
    }
}
