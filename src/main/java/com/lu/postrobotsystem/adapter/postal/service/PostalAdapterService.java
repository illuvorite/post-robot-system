package com.lu.postrobotsystem.adapter.postal.service;

import com.lu.postrobotsystem.adapter.postal.config.PostalProperties;
import com.lu.postrobotsystem.adapter.postal.exception.PostalApiException;
import com.lu.postrobotsystem.adapter.postal.exception.PostalRetryableException;
import com.lu.postrobotsystem.adapter.postal.model.SessionHeader;
import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;
import com.lu.postrobotsystem.adapter.postal.model.enums.ServiceCode;
import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;
import com.lu.postrobotsystem.adapter.postal.spi.PostalApi;
import com.lu.postrobotsystem.common.util.SignUtil;
import com.lu.postrobotsystem.common.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮政适配器服务门面
 * <p>
 * 业务层统一入口，封装 PostalApi 的调用细节：
 * <ul>
 *   <li>自动构建 SessionHeader（注入机构编码、平台编码等配置）</li>
 *   <li>统一签名计算（委托 SignUtil）</li>
 *   <li>统一异常包装（PostalApiException → 带上下文日志）</li>
 *   <li>调用耗时监控日志</li>
 *   <li>错误码转换</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostalAdapterService {

    private final PostalApi postalApi;
    private final PostalProperties postalProperties;

    private static final DateTimeFormatter REQ_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String VERSION = "YY-1.0";
    private static final String ACTION_CODE = "0";

    /**
     * F1：邮件资费查询
     */
    public PostageQueryResponse queryPostage(PostageQueryBody body) {
        long start = System.currentTimeMillis();
        SessionHeader header = buildSessionHeader(ServiceCode.POSTAGE_QUERY, toJson(body));
        PostageQueryRequest request = PostageQueryRequest.builder()
                .sessionHeader(header).sessionBody(body).build();
        try {
            PostageQueryResponse response = postalApi.queryPostage(request);
            log.info("[邮政适配层] F1资费查询完成: weight={}, totalPostage={}, 耗时={}ms",
                    body.getWeight(), response.getTotalPostage(), System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[邮政适配层] F1资费查询失败: weight={}, 耗时={}ms", body.getWeight(),
                    System.currentTimeMillis() - start, e);
            throw wrapException("资费查询", e);
        }
    }

    /**
     * F2：邮件号码生成
     */
    public MailNumberResponse generateMailNumber(MailNumberBody body) {
        long start = System.currentTimeMillis();
        SessionHeader header = buildSessionHeader(ServiceCode.MAIL_NUMBER_GEN, toJson(body));
        MailNumberRequest request = MailNumberRequest.builder()
                .sessionHeader(header).sessionBody(body).build();
        try {
            MailNumberResponse response = postalApi.generateMailNumber(request);
            log.info("[邮政适配层] F2号码生成完成: mailNo={}, 耗时={}ms",
                    response.getMailNo(), System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[邮政适配层] F2号码生成失败: 耗时={}ms", System.currentTimeMillis() - start, e);
            throw wrapException("号码生成", e);
        }
    }

    /**
     * F3：收寄订单提交
     */
    public OrderSubmitResponse submitOrder(OrderSubmitBody body) {
        long start = System.currentTimeMillis();
        try {
            OrderSubmitResponse response = postalApi.submitOrder(body);
            log.info("[邮政适配层] F3收寄订单提交完成: mailNo={}, transId={}, 耗时={}ms",
                    response.getMailNo(), response.getTransactionId(), System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[邮政适配层] F3收寄订单提交失败: mailNo={}, 耗时={}ms",
                    body.getMailNo(), System.currentTimeMillis() - start, e);
            throw wrapException("收寄订单提交", e);
        }
    }

    /**
     * F4：生成收款二维码
     */
    public QrCodeResponse generateQrCode(String orderNo, String amount, String paymentFlowNo) {
        QrCodeBody body = QrCodeBody.builder()
                .orgCode(postalProperties.getPlatform().getOrgCode())
                .deskCode(postalProperties.getPlatform().getDeskCode())
                .staffCode("ROBOT01")
                .queryNo("QRY_" + paymentFlowNo)
                .orderNo(orderNo)
                .paymentFlowNo(paymentFlowNo)
                .amount(amount)
                .build();

        long start = System.currentTimeMillis();
        try {
            QrCodeResponse response = postalApi.generateQrCode(body);
            log.info("[邮政适配层] F4二维码生成完成: orderNo={}, 耗时={}ms",
                    orderNo, System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[邮政适配层] F4二维码生成失败: orderNo={}, 耗时={}ms",
                    orderNo, System.currentTimeMillis() - start, e);
            throw wrapException("二维码生成", e);
        }
    }

    /**
     * F5：支付状态查询
     */
    public PaymentStatusResponse queryPaymentStatus(String queryNo, String paymentFlowNo) {
        PaymentStatusBody body = PaymentStatusBody.builder()
                .queryNo(queryNo)
                .orgCode(postalProperties.getPlatform().getOrgCode())
                .paymentFlowNo(paymentFlowNo)
                .build();

        long start = System.currentTimeMillis();
        try {
            PaymentStatusResponse response = postalApi.queryPaymentStatus(body);
            log.info("[邮政适配层] F5支付状态查询完成: queryNo={}, status={}, 耗时={}ms",
                    queryNo, response.getPayStatus(), System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[邮政适配层] F5支付状态查询失败: queryNo={}, 耗时={}ms",
                    queryNo, System.currentTimeMillis() - start, e);
            throw wrapException("支付状态查询", e);
        }
    }

    /**
     * 构建 SessionHeader（含签名计算）
     */
    private SessionHeader buildSessionHeader(ServiceCode serviceCode, String sessionBodyJson) {
        String reqTime = LocalDateTime.now().format(REQ_TIME_FMT);
        String transactionId = TransactionIdGenerator.generate();

        String digitalSign = SignUtil.sign(
                serviceCode.getCode(), VERSION, ACTION_CODE,
                transactionId,
                postalProperties.getPlatform().getSrcSysId(),
                postalProperties.getPlatform().getDstSysId(),
                reqTime, sessionBodyJson,
                postalProperties.getApi().getSignSecret()
        );

        return SessionHeader.builder()
                .serviceCode(serviceCode.getCode())
                .version(VERSION)
                .actionCode(ACTION_CODE)
                .transactionId(transactionId)
                .srcSysId(postalProperties.getPlatform().getSrcSysId())
                .dstSysId(postalProperties.getPlatform().getDstSysId())
                .digitalSign(digitalSign)
                .reqTime(reqTime)
                .build();
    }

    /**
     * JSON 序列化（用于签名计算）
     */
    private String toJson(Object obj) {
        return cn.hutool.json.JSONUtil.toJsonStr(obj);
    }

    /**
     * 统一异常包装
     */
    private RuntimeException wrapException(String operation, Exception e) {
        if (e instanceof PostalApiException pe) {
            return pe;
        }
        if (e instanceof PostalRetryableException pr) {
            return pr;
        }
        return new PostalApiException(PostalErrorCode.UNKNOWN,
                "邮政接口[" + operation + "]调用异常: " + e.getMessage(), e);
    }
}
