package com.lu.postrobotsystem.adapter.postal;

import com.lu.postrobotsystem.adapter.postal.config.PostalProperties;
import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;
import com.lu.postrobotsystem.adapter.postal.spi.PostalApiMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮政 API Mock 实现单元测试
 * <p>
 * 验证全部 5 个接口的 Mock 响应结构正确性，
 * 以及支付状态模拟机的状态流转逻辑。
 * </p>
 */
class PostalApiMockTest {

    private PostalApiMock postalApiMock;

    @BeforeEach
    void setUp() {
        PostalProperties props = new PostalProperties();
        props.getMock().setPaymentDelayMs(100); // 快速模拟支付成功
        props.getPlatform().setOrgCode("AHFY01");
        props.getPlatform().setDeskCode("ROBOT01");
        props.getApi().setSignSecret("test-secret");
        postalApiMock = new PostalApiMock(props);
        postalApiMock.init();
    }

    @Test
    void testQueryPostage_shouldReturnValidResponse() {
        // 给定
        PostageQueryBody body = PostageQueryBody.builder()
                .productCode("YT")
                .weight(500)
                .insured(true)
                .sendProvince("安徽省")
                .sendCity("合肥市")
                .destProvince("北京市")
                .destCity("北京市")
                .build();
        PostageQueryRequest request = PostageQueryRequest.builder()
                .sessionBody(body)
                .build();

        // 执行
        PostageQueryResponse response = postalApiMock.queryPostage(request);

        // 验证
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalPostage());
        assertTrue(response.getTotalPostage().compareTo(BigDecimal.ZERO) > 0, "资费应大于0");
        assertNotNull(response.getFreight());
        assertTrue(response.getInsuranceFee().compareTo(BigDecimal.ZERO) > 0, "保价费应大于0");
    }

    @Test
    void testGenerateMailNumber_shouldReturnUniqueMailNo() {
        // 给定
        MailNumberBody body = MailNumberBody.builder()
                .provinceCode("340000")
                .orgCode("AHFY01")
                .productCode("YT")
                .build();
        MailNumberRequest request1 = MailNumberRequest.builder().sessionBody(body).build();
        MailNumberRequest request2 = MailNumberRequest.builder().sessionBody(body).build();

        // 执行
        MailNumberResponse response1 = postalApiMock.generateMailNumber(request1);
        MailNumberResponse response2 = postalApiMock.generateMailNumber(request2);

        // 验证
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertNotNull(response1.getMailNo());
        assertNotNull(response2.getMailNo());
        assertNotEquals(response1.getMailNo(), response2.getMailNo(), "生成的邮件号码必须唯一");
    }

    @Test
    void testSubmitOrder_shouldReturnTransactionId() {
        // 给定
        OrderSubmitBody body = OrderSubmitBody.builder()
                .mailNo("202600000001")
                .weight(500)
                .pieceCount(1)
                .productCode("YT")
                .productName("标准快递")
                .totalPostage(new BigDecimal("1200"))
                .build();

        // 执行
        OrderSubmitResponse response = postalApiMock.submitOrder(body);

        // 验证
        assertTrue(response.isSuccess());
        assertNotNull(response.getTransactionId());
        assertEquals(body.getMailNo(), response.getMailNo());
        assertEquals(body.getTotalPostage(), response.getTotalPostage());
        assertNotNull(response.getPostageDetails());
        assertFalse(response.getPostageDetails().isEmpty());
    }

    @Test
    void testGenerateQrCode_shouldReturnQrUrlAndQueryNo() {
        // 给定
        QrCodeBody body = QrCodeBody.builder()
                .orderNo("ORD001")
                .paymentFlowNo("PAY001")
                .amount("1200")
                .orgCode("AHFY01")
                .queryNo("QRY_PAY001")
                .build();

        // 执行
        QrCodeResponse response = postalApiMock.generateQrCode(body);

        // 验证
        assertTrue(response.isSuccess());
        assertNotNull(response.getQrCodeUrl());
        assertTrue(response.getQrCodeUrl().contains("PAY001"));
        assertNotNull(response.getPayQueryNo());
    }

    @Test
    void testPaymentStatusMachine_shouldTransitionFromPayingToSuccess() throws InterruptedException {
        // 给定：先生成二维码（初始化支付状态）
        QrCodeBody qrBody = QrCodeBody.builder()
                .orderNo("ORD002")
                .paymentFlowNo("PAY002")
                .amount("2000")
                .queryNo("QRY_PAY002")
                .build();
        postalApiMock.generateQrCode(qrBody);

        PaymentStatusBody statusBody = PaymentStatusBody.builder()
                .queryNo("QRY_PAY002")
                .paymentFlowNo("PAY002")
                .build();

        // 执行 & 验证：首次查询应为支付中
        PaymentStatusResponse firstQuery = postalApiMock.queryPaymentStatus(statusBody);
        assertEquals("00", firstQuery.getPayStatus(), "首次查询应为支付中");

        // 等待模拟支付延迟
        Thread.sleep(150); // 100ms + buffer

        // 再次查询应为支付成功
        PaymentStatusResponse secondQuery = postalApiMock.queryPaymentStatus(statusBody);
        assertEquals("01", secondQuery.getPayStatus(), "延迟后查询应为支付成功");
        assertNotNull(secondQuery.getPaidTime(), "支付成功应返回支付完成时间");
    }

    @Test
    void testMockStateReset_shouldClearAllState() {
        // 给定：创建一些状态
        QrCodeBody qrBody = QrCodeBody.builder()
                .orderNo("ORD003")
                .paymentFlowNo("PAY003")
                .amount("3000")
                .queryNo("QRY_PAY003")
                .build();
        postalApiMock.generateQrCode(qrBody);

        // 验证：有状态
        assertFalse(postalApiMock.getPaymentStatusSnapshot().isEmpty());

        // 执行：重置
        postalApiMock.resetMockState();

        // 验证：状态已清空
        assertTrue(postalApiMock.getPaymentStatusSnapshot().isEmpty());
    }
}
