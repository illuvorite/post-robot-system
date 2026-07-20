package com.lu.postrobotsystem.adapter.postal;

import com.lu.postrobotsystem.common.util.SignUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮政接口签名算法单元测试
 * <p>
 * 验证 SignUtil 的签名计算是否正确可靠。
 * </p>
 */
class SignUtilTest {

    @Test
    void testSign_shouldProduceBase64EncodedMd5() {
        // 给定
        String serviceCode = "F4";
        String version = "YY-1.0";
        String actionCode = "0";
        String transactionId = "POSTR202607201234567890000000001";
        String srcSysId = "POSTR";
        String dstSysId = "XYDYYQDXT";
        String reqTime = "20260720123456";
        String sessionBodyJson = "{\"orderNo\":\"ORD001\",\"amount\":\"1200\"}";
        String secretKey = "test-secret-key";

        // 执行
        String sign = SignUtil.sign(serviceCode, version, actionCode,
                transactionId, srcSysId, dstSysId, reqTime, sessionBodyJson, secretKey);

        // 验证
        assertNotNull(sign, "签名结果不能为空");
        assertFalse(sign.isEmpty(), "签名结果不能为空字符串");
        assertTrue(sign.length() > 20, "Base64 编码的 MD5 摘要长度应大于 20");
    }

    @Test
    void testSign_shouldBeDeterministic() {
        // 给定相同参数
        String sign1 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{}", "secret");
        String sign2 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{}", "secret");

        // 验证：相同输入必须产生相同签名
        assertEquals(sign1, sign2, "相同输入必须产生相同签名");
    }

    @Test
    void testSign_differentInputs_shouldProduceDifferentSignatures() {
        // 不同 secret 应该产生不同签名
        String sign1 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{}", "secret1");
        String sign2 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{}", "secret2");

        assertNotEquals(sign1, sign2, "不同密钥必须产生不同签名");
    }

    @Test
    void testSign_differentSessionBody_shouldProduceDifferentSignatures() {
        String sign1 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{\"amount\":100}", "secret");
        String sign2 = SignUtil.sign("F1", "YY-1.0", "0",
                "POSTR202607201234567890000000001", "POSTR", "XYDYYQDXT",
                "20260720123456", "{\"amount\":200}", "secret");

        assertNotEquals(sign1, sign2, "不同请求体必须产生不同签名");
    }
}
