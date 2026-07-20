package com.lu.postrobotsystem.adapter.postal.spi;

import com.lu.postrobotsystem.adapter.postal.config.PostalProperties;
import com.lu.postrobotsystem.adapter.postal.exception.PostalApiException;
import com.lu.postrobotsystem.adapter.postal.exception.PostalRetryableException;
import com.lu.postrobotsystem.adapter.postal.model.SessionHeader;
import com.lu.postrobotsystem.adapter.postal.model.YYRoot;
import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;
import com.lu.postrobotsystem.adapter.postal.model.enums.ServiceCode;
import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;
import com.lu.postrobotsystem.common.util.SignUtil;
import com.lu.postrobotsystem.common.util.TransactionIdGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 邮政 API HTTP 客户端实现
 * <p>
 * 当 postal.mock.enabled=false 时生效。
 * 基于 RestTemplate 调用真实的邮政 HTTP 接口，包含：
 * <ul>
 *   <li>YYRoot 请求报文组装 + JSON 序列化</li>
 *   <li>统一签名计算（SignUtil）</li>
 *   <li>响应验签</li>
 *   <li>超时重试（可配置次数）</li>
 *   <li>错误码映射</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "postal.mock.enabled", havingValue = "false")
public class PostalApiHttpClient implements PostalApi {

    private final PostalProperties postalProperties;
    private final RestTemplate restTemplate;

    private static final DateTimeFormatter REQ_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String VERSION = "YY-1.0";
    private static final String ACTION_CODE = "0";

    @PostConstruct
    public void init() {
        log.info("邮政API-HTTP客户端初始化: baseUrl={}, retryCount={}, connectTimeout={}",
                postalProperties.getApi().getBaseUrl(),
                postalProperties.getApi().getRetryCount(),
                postalProperties.getApi().getConnectTimeout());
    }

    @Override
    public PostageQueryResponse queryPostage(PostageQueryRequest request) {
        // 构建请求 YYRoot
        YYRoot<PostageQueryBody> yyRoot = buildRequest(request.getSessionHeader(), request.getSessionBody());
        // TODO: 实际 HTTP 调用 + 验签 + 解析
        // YYRoot<PostageQueryResponse> response = doPost(yyRoot, PostageQueryResponse.class);
        log.warn("[邮政API-HTTP] F1资费查询: HTTP调用未实现（Mock模式已关闭但HTTP客户端尚未配置）");
        throw new PostalApiException(PostalErrorCode.SERVICE_CLOSED, "HTTP客户端未实现");
    }

    @Override
    public MailNumberResponse generateMailNumber(MailNumberRequest request) {
        YYRoot<MailNumberBody> yyRoot = buildRequest(request.getSessionHeader(), request.getSessionBody());
        log.warn("[邮政API-HTTP] F2号码生成: HTTP调用未实现");
        throw new PostalApiException(PostalErrorCode.SERVICE_CLOSED, "HTTP客户端未实现");
    }

    @Override
    public OrderSubmitResponse submitOrder(OrderSubmitBody body) {
        YYRoot<OrderSubmitBody> yyRoot = buildRequest(null, body);
        log.warn("[邮政API-HTTP] F3收寄订单提交: HTTP调用未实现");
        throw new PostalApiException(PostalErrorCode.SERVICE_CLOSED, "HTTP客户端未实现");
    }

    @Override
    public QrCodeResponse generateQrCode(QrCodeBody body) {
        YYRoot<QrCodeBody> yyRoot = buildRequest(null, body);
        log.warn("[邮政API-HTTP] F4收款二维码: HTTP调用未实现");
        throw new PostalApiException(PostalErrorCode.SERVICE_CLOSED, "HTTP客户端未实现");
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(PaymentStatusBody body) {
        YYRoot<PaymentStatusBody> yyRoot = buildRequest(null, body);
        log.warn("[邮政API-HTTP] F5支付状态查询: HTTP调用未实现");
        throw new PostalApiException(PostalErrorCode.SERVICE_CLOSED, "HTTP客户端未实现");
    }

    /**
     * 构建 YYRoot 请求报文（含计算签名）
     */
    private <T> YYRoot<T> buildRequest(SessionHeader header, T body) {
        String reqTime = LocalDateTime.now().format(REQ_TIME_FMT);
        String transactionId = TransactionIdGenerator.generate();

        // 反查 ServiceCode（根据 body 类型）
        ServiceCode serviceCode = resolveServiceCode(body);
        String serviceCodeStr = serviceCode != null ? serviceCode.getCode() : "UNKNOWN";

        // 序列化 body 为 JSON
        String sessionBodyJson = cn.hutool.json.JSONUtil.toJsonStr(body);

        // 计算签名
        String digitalSign = SignUtil.sign(
                serviceCodeStr, VERSION, ACTION_CODE,
                transactionId,
                postalProperties.getPlatform().getSrcSysId(),
                postalProperties.getPlatform().getDstSysId(),
                reqTime, sessionBodyJson,
                postalProperties.getApi().getSignSecret()
        );

        // 构建会话头
        SessionHeader finalHeader = header != null ? header : new SessionHeader();
        if (finalHeader.getServiceCode() == null) finalHeader.setServiceCode(serviceCodeStr);
        if (finalHeader.getVersion() == null) finalHeader.setVersion(VERSION);
        if (finalHeader.getActionCode() == null) finalHeader.setActionCode(ACTION_CODE);
        if (finalHeader.getTransactionId() == null) finalHeader.setTransactionId(transactionId);
        if (finalHeader.getSrcSysId() == null) finalHeader.setSrcSysId(postalProperties.getPlatform().getSrcSysId());
        if (finalHeader.getDstSysId() == null) finalHeader.setDstSysId(postalProperties.getPlatform().getDstSysId());
        if (finalHeader.getReqTime() == null) finalHeader.setReqTime(reqTime);
        finalHeader.setDigitalSign(digitalSign);

        log.debug("邮政API请求签名: serviceCode={}, transactionId={}, sign={}",
                serviceCodeStr, transactionId, digitalSign.substring(0, Math.min(8, digitalSign.length())));

        return YYRoot.of(finalHeader, body);
    }

    /**
     * 根据 body 类型反查 ServiceCode
     */
    private <T> ServiceCode resolveServiceCode(T body) {
        if (body instanceof PostageQueryBody) return ServiceCode.POSTAGE_QUERY;
        if (body instanceof MailNumberBody) return ServiceCode.MAIL_NUMBER_GEN;
        if (body instanceof OrderSubmitBody) return ServiceCode.ORDER_SUBMIT;
        if (body instanceof QrCodeBody) return ServiceCode.QR_CODE_GEN;
        if (body instanceof PaymentStatusBody) return ServiceCode.PAYMENT_STATUS_QUERY;
        return null;
    }

    /**
     * 执行 HTTP POST 调用 + 重试 + 验签
     * <pre>
     * 1. 构建 HttpEntity<YYRoot<T>>（JSON 序列化）
     * 2. RestTemplate.postForObject(url, request, String.class)
     * 3. 解析响应 JSON 为 YYRoot
     * 4. 验签：重新计算签名并与响应的 digitalSign 比较
     * 5. 错误码映射：PostalErrorCode.getByPostalCode()
     * 6. 可重试错误 → PostalRetryableException
     * 7. 不可重试错误 → PostalApiException
     * </pre>
     */
    private <T, R> R doPost(YYRoot<T> request, Class<R> responseType) {
        String url = postalProperties.getApi().getBaseUrl();
        int retryCount = postalProperties.getApi().getRetryCount();

        // 构建请求头
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("transactionId", request.getSessionHeader().getTransactionId());
        headers.set("reqTime", request.getSessionHeader().getReqTime());

        // 序列化请求体
        String requestBody = cn.hutool.json.JSONUtil.toJsonStr(request);
        org.springframework.http.HttpEntity<String> httpEntity = new org.springframework.http.HttpEntity<>(requestBody, headers);

        // 重试调用
        Exception lastException = null;
        for (int i = 0; i <= retryCount; i++) {
            try {
                // 发送 POST 请求
                String responseBody = restTemplate.postForObject(url, httpEntity, String.class);
                if (responseBody == null) {
                    throw new PostalRetryableException(PostalErrorCode.SYSTEM_ERROR, "邮政接口返回空响应");
                }

                // 解析响应
                var responseJson = cn.hutool.json.JSONUtil.parseObj(responseBody);
                var sessionHeaderJson = responseJson.getJSONObject("sessionHeader");

                if (sessionHeaderJson == null) {
                    throw new PostalApiException(PostalErrorCode.PROTOCOL_ENCODE_ERROR, "响应缺少 SessionHeader");
                }

                // 验签：重新计算签名并与响应的 digitalSign 比较
                String respSign = sessionHeaderJson.getStr("digitalSign");
                String respServiceCode = sessionHeaderJson.getStr("serviceCode");
                String respTransactionId = sessionHeaderJson.getStr("transactionId");
                String respReqTime = sessionHeaderJson.getStr("reqTime");

                // 响应验签（重新计算签名对比）
                String sessionBodyStr = responseJson.getStr("sessionBody", "{}");
                String calculatedSign = SignUtil.sign(
                        respServiceCode != null ? respServiceCode : "",
                        VERSION, ACTION_CODE,
                        respTransactionId != null ? respTransactionId : "",
                        postalProperties.getPlatform().getDstSysId(), // 响应中 SrcSysId 与请求的 DstSysId 对应
                        postalProperties.getPlatform().getSrcSysId(),
                        respReqTime != null ? respReqTime : "",
                        sessionBodyStr,
                        postalProperties.getApi().getSignSecret()
                );

                if (respSign != null && !respSign.equals(calculatedSign)) {
                    log.warn("邮政接口响应签名验证失败: transactionId={}, respSign={}...",
                            respTransactionId, respSign.substring(0, Math.min(8, respSign.length())));
                    throw new PostalApiException(PostalErrorCode.SIGN_ERROR, "响应签名验证失败");
                }

                // 检查邮政侧错误码
                String respCode = sessionHeaderJson.getStr("respCode", "0000");
                if (!"0000".equals(respCode)) {
                    PostalErrorCode errorCode = PostalErrorCode.getByPostalCode(respCode);
                    String errMsg = sessionHeaderJson.getStr("respMsg", errorCode.getMessage());

                    // 可重试错误
                    if (errorCode == PostalErrorCode.QUOTA_EXCEEDED
                            || errorCode == PostalErrorCode.SERVICE_CLOSED
                            || errorCode == PostalErrorCode.HANDSHAKE_FAIL) {
                        throw new PostalRetryableException(errorCode, errMsg);
                    }
                    throw new PostalApiException(errorCode, errMsg);
                }

                // 提取 SessionBody 并转换为目标类型
                var sessionBody = responseJson.get("sessionBody");
                if (sessionBody == null) {
                    throw new PostalApiException(PostalErrorCode.PROTOCOL_ENCODE_ERROR, "响应缺少 SessionBody");
                }

                // 将 JSON 转换为目标类型
                String sessionBodyJson = cn.hutool.json.JSONUtil.toJsonStr(sessionBody);
                return cn.hutool.json.JSONUtil.toBean(sessionBodyJson, responseType);

            } catch (PostalApiException e) {
                // 不可重试错误，直接抛出
                throw e;
            } catch (Exception e) {
                lastException = e;
                log.warn("邮政接口调用失败(第{}次): {}", i + 1, e.getMessage());
                if (i < retryCount) {
                    try {
                        Thread.sleep(1000L * (i + 1)); // 递增退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new PostalRetryableException(PostalErrorCode.HANDSHAKE_FAIL,
                "邮政接口调用失败，已重试" + retryCount + "次", lastException);
    }
}
