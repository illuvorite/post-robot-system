package com.lu.postrobotsystem.common.util;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * 邮政接口签名工具类
 */
public class SignUtil {

    /**
     * 计算邮政接口请求签名
     * <p>
     * 按照邮政接口规范拼接所有请求参数和密钥，依次经过 MD5 摘要和 Base64 编码后返回签名结果。
     * </p>
     *
     * @param serviceCode     接口服务代码，标识具体的业务服务
     * @param version         接口版本号，用于接口版本管理
     * @param actionCode      操作代码，标识具体的业务操作
     * @param transactionId   事务唯一标识，由 {@link TransactionIdGenerator#generate()} 生成
     * @param srcSysId        源系统ID，标识发起请求的系统
     * @param dstSysId        目标系统ID，标识接收请求的系统
     * @param reqTime         请求时间，格式为 yyyyMMddHHmmss
     * @param sessionBodyJson 请求体内容的 JSON 字符串
     * @param secretKey       双方约定的签名密钥
     * @return 计算得到的签名字符串（Base64 编码的 MD5 摘要）
     * @throws RuntimeException 当签名计算过程中发生异常时抛出
     */
    public static String sign(String serviceCode, String version, String actionCode,
                              String transactionId, String srcSysId, String dstSysId,
                              String reqTime, String sessionBodyJson, String secretKey) {
        try {
            // 按照邮政接口规范拼接原始字符串
            String raw = serviceCode + version + actionCode + transactionId
                       + srcSysId + dstSysId + reqTime + sessionBodyJson + secretKey;
            // 计算 MD5 摘要
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(raw.getBytes("UTF-8"));
            // Base64 编码后返回
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            // 包装为运行时异常抛出，由调用方统一处理
            throw new RuntimeException("签名计算失败", e);
        }
    }
}
