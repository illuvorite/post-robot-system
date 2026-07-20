package com.lu.postrobotsystem.adapter.postal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮政接口会话头（SessionHeader）
 * <p>
 * 所有邮政接口请求/响应共用同一套会话头结构，包含服务代码、版本、
 * 事务ID、签名等字段。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionHeader {

    /** 服务代码（如 F1 资费查询、F4 二维码生成），对应 {@link com.lu.postrobotsystem.adapter.postal.model.enums.ServiceCode} */
    private String serviceCode;

    /** 接口版本号（如 YY-1.0） */
    private String version;

    /** 操作代码（请求填 0） */
    private String actionCode;

    /** 全局唯一事务 ID，格式：5位平台编码 + 17位日期(yyyyMMddHHmmssSSS) + 10位流水号 = 32位 */
    private String transactionId;

    /** 发起方系统 ID */
    private String srcSysId;

    /** 目标方系统 ID（固定值 XYDYYQDXT） */
    private String dstSysId;

    /** 数字签名（MD5 + BASE64） */
    private String digitalSign;

    /** 请求时间（yyyyMMddHHmmss） */
    private String reqTime;
}
