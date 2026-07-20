package com.lu.postrobotsystem.adapter.postal;

import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮政错误码映射单元测试
 * <p>
 * 验证 PostalErrorCode 能正确映射邮政侧错误码到本地错误码，
 * 且未知错误码能正确回退到 UNKNOWN。
 * </p>
 */
class PostalErrorCodeTest {

    @Test
    void testKnownPostalCode_shouldMapCorrectly() {
        assertEquals(PostalErrorCode.SUCCESS, PostalErrorCode.getByPostalCode("0000"));
        assertEquals(PostalErrorCode.DECRYPT_ERROR, PostalErrorCode.getByPostalCode("9009"));
        assertEquals(PostalErrorCode.SIGN_ERROR, PostalErrorCode.getByPostalCode("1002"));
        assertEquals(PostalErrorCode.QUOTA_EXCEEDED, PostalErrorCode.getByPostalCode("1006"));
        assertEquals(PostalErrorCode.WHITELIST_FORBIDDEN, PostalErrorCode.getByPostalCode("1007"));
    }

    @Test
    void testUnknownPostalCode_shouldFallbackToUnknown() {
        assertEquals(PostalErrorCode.UNKNOWN, PostalErrorCode.getByPostalCode("99999"));
        assertEquals(PostalErrorCode.UNKNOWN, PostalErrorCode.getByPostalCode(""));
        assertEquals(PostalErrorCode.UNKNOWN, PostalErrorCode.getByPostalCode(null));
    }

    @Test
    void testErrorCode_shouldImplementIErrorCode() {
        assertTrue(PostalErrorCode.SUCCESS.getCode() == 0);
        assertTrue(PostalErrorCode.SIGN_ERROR.getCode() == 40110);
        assertTrue(PostalErrorCode.SERVICE_CLOSED.getCode() == 50005);
        assertTrue(PostalErrorCode.QUOTA_EXCEEDED.getCode() == 60000);
    }

    @Test
    void testAllErrorCodes_shouldHaveNonEmptyPostalCode() {
        for (PostalErrorCode code : PostalErrorCode.values()) {
            assertNotNull(code.getPostalCode(), "错误码 " + code.name() + " 的 postalCode 不能为空");
            assertFalse(code.getPostalCode().isEmpty(), "错误码 " + code.name() + " 的 postalCode 不能为空字符串");
        }
    }
}
