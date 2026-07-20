package com.lu.postrobotsystem.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务ID生成器
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
        // 原子自增获取下一个序列号（达到上限自动归零，无竞态条件）
        long seq = SEQ.updateAndGet(v -> v >= 9999999999L ? 0 : v + 1);
        // 拼接 平台编码 + 时间戳 + 10位补零流水号
        return PLATFORM_CODE
             + LocalDateTime.now().format(FORMATTER)
             + String.format("%010d", seq);
    }
}
