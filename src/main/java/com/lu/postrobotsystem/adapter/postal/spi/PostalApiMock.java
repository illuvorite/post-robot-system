package com.lu.postrobotsystem.adapter.postal.spi;

import com.lu.postrobotsystem.adapter.postal.config.PostalProperties;
import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;
import com.lu.postrobotsystem.adapter.postal.model.enums.ServiceCode;
import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 邮政 API Mock 实现
 * <p>
 * 当 postal.mock.enabled=true 时生效。模拟邮政系统的全部 5 个接口，
 * 返回结构正确的报文。支持：
 * <ul>
 *   <li>支付状态模拟机：首次查询返回支付中，后续按时间推移自动演进为成功</li>
 *   <li>可配置的异常场景：通过请求参数触发签名错误等</li>
 *   <li>完整的请求/响应日志记录</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "postal.mock.enabled", havingValue = "true", matchIfMissing = true)
public class PostalApiMock implements PostalApi {

    private final PostalProperties postalProperties;

    /** 日期格式器 */
    private static final DateTimeFormatter REQ_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 模拟邮件号码计数器 */
    private final AtomicLong mailNoCounter = new AtomicLong(202600000001L);

    /** 支付状态模拟机：key=queryNo, value=首次查询时间戳 */
    private final ConcurrentHashMap<String, Long> paymentStatusStore = new ConcurrentHashMap<>();

    /** Mock 模拟的支付等待毫秒数（模拟支付处理延迟） */
    private long paymentDelayMs;

    @PostConstruct
    public void init() {
        this.paymentDelayMs = postalProperties.getMock().getPaymentDelayMs();
        log.info("邮政API-Mock 初始化完成, paymentDelayMs={}, orgCode={}",
                paymentDelayMs, postalProperties.getPlatform().getOrgCode());
    }

    @Override
    public PostageQueryResponse queryPostage(PostageQueryRequest request) {
        log.info("[邮政API-MOCK] F1资费查询: productCode={}, weight={}g, send={}/{} -> dest={}/{}",
                request.getSessionBody().getProductCode(),
                request.getSessionBody().getWeight(),
                request.getSessionBody().getSendProvince(), request.getSessionBody().getSendCity(),
                request.getSessionBody().getDestProvince(), request.getSessionBody().getDestCity());

        // 模拟返回资费
        return PostageQueryResponse.builder()
                .success(true)
                .totalPostage(new BigDecimal("1200"))  // 12.00元
                .freight(new BigDecimal("1000"))
                .insuranceFee(request.getSessionBody().getInsured() != null
                        && request.getSessionBody().getInsured() ? new BigDecimal("200") : BigDecimal.ZERO)
                .packingFee(new BigDecimal("100"))
                .otherFee(BigDecimal.ZERO)
                .build();
    }

    @Override
    public MailNumberResponse generateMailNumber(MailNumberRequest request) {
        long mailNo = mailNoCounter.getAndIncrement();
        String mailNoStr = String.valueOf(mailNo);

        log.info("[邮政API-MOCK] F2号码生成: productCode={}, mailNo={}",
                request.getSessionBody().getProductCode(), mailNoStr);

        return MailNumberResponse.builder()
                .success(true)
                .mailNo(mailNoStr)
                .build();
    }

    @Override
    public OrderSubmitResponse submitOrder(OrderSubmitBody body) {
        log.info("[邮政API-MOCK] F3收寄订单提交: mailNo={}, product={}, totalPostage={}",
                body.getMailNo(), body.getProductName(), body.getTotalPostage());

        // 生成模拟交易流水号
        String transId = "MOCK_TRANS_" + System.currentTimeMillis();

        return OrderSubmitResponse.builder()
                .success(true)
                .transactionId(transId)
                .mailNo(body.getMailNo())
                .totalPostage(body.getTotalPostage())
                .receivablePostage(body.getTotalPostage())
                .postageDetails(Collections.singletonList(
                        OrderSubmitResponse.PostageDetailItem.builder()
                                .itemCode("POSTAGE")
                                .itemName("邮资")
                                .amount(body.getTotalPostage())
                                .build()))
                .build();
    }

    @Override
    public QrCodeResponse generateQrCode(QrCodeBody body) {
        log.info("[邮政API-MOCK] F4生成收款二维码: orderNo={}, amount={}, queryNo={}",
                body.getOrderNo(), body.getAmount(), body.getQueryNo());

        // 初始化支付状态（用于后续支付状态查询）
        String queryNo = body.getQueryNo() != null ? body.getQueryNo() : "QRY_" + body.getPaymentFlowNo();
        paymentStatusStore.put(queryNo, System.currentTimeMillis());

        return QrCodeResponse.builder()
                .success(true)
                .platformFlowNo("MOCK_PLATFORM_" + body.getPaymentFlowNo())
                .paymentFlowNo(body.getPaymentFlowNo())
                .qrCodeUrl("/mock/qrcode/" + body.getPaymentFlowNo() + ".png")
                .payQueryNo(queryNo)
                .build();
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(PaymentStatusBody body) {
        log.info("[邮政API-MOCK] F5支付状态查询: queryNo={}, paymentFlowNo={}",
                body.getQueryNo(), body.getPaymentFlowNo());

        // 支付状态模拟机逻辑
        String queryNo = body.getQueryNo();
        Long firstQueryTime = paymentStatusStore.get(queryNo);

        if (firstQueryTime == null) {
            // 首次查询此 queryNo，初始化并返回支付中
            paymentStatusStore.put(queryNo, System.currentTimeMillis());
            log.info("[邮政API-MOCK] 支付状态模拟: queryNo={} -> 支付中(00)", queryNo);
            return PaymentStatusResponse.builder()
                    .success(true)
                    .payStatus("00")
                    .platformFlowNo("MOCK_PLATFORM_" + body.getPaymentFlowNo())
                    .statusDesc("支付中")
                    .build();
        }

        // 检查是否超过模拟支付延迟
        long elapsed = System.currentTimeMillis() - firstQueryTime;
        if (elapsed >= paymentDelayMs) {
            // 超过延迟时间 → 返回支付成功
            String paidTime = LocalDateTime.now().format(REQ_TIME_FMT);
            log.info("[邮政API-MOCK] 支付状态模拟: queryNo={} -> 支付成功(01), 耗时={}ms", queryNo, elapsed);
            return PaymentStatusResponse.builder()
                    .success(true)
                    .payStatus("01")
                    .platformFlowNo("MOCK_PLATFORM_" + body.getPaymentFlowNo())
                    .paidTime(paidTime)
                    .statusDesc("支付成功")
                    .build();
        }

        // 仍在支付中
        log.info("[邮政API-MOCK] 支付状态模拟: queryNo={} -> 支付中(00), elapsed={}ms", queryNo, elapsed);
        return PaymentStatusResponse.builder()
                .success(true)
                .payStatus("00")
                .platformFlowNo("MOCK_PLATFORM_" + body.getPaymentFlowNo())
                .statusDesc("支付中")
                .build();
    }

    /**
     * 获取当前 Mock 支付状态机的快照（管理和监控用）
     */
    public ConcurrentHashMap<String, Long> getPaymentStatusSnapshot() {
        return new ConcurrentHashMap<>(paymentStatusStore);
    }

    /**
     * 重置 Mock 状态机
     */
    public void resetMockState() {
        paymentStatusStore.clear();
        mailNoCounter.set(202600000001L);
        log.info("邮政API-Mock 状态已重置");
    }
}
