package com.lu.postrobotsystem.adapter.postal.spi;

import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;

/**
 * 邮政系统 API 接口（SPI 契约）
 * <p>
 * 定义 5 个邮政业务接口的抽象方法。可通过 {@link PostalApiMock}（Mock 模式）
 * 或 {@link PostalApiHttpClient}（真实 HTTP 模式）实现。
 * 业务代码通过 {@link com.lu.postrobotsystem.adapter.postal.service.PostalAdapterService}
 * 统一调用，无需感知具体实现。
 * </p>
 */
public interface PostalApi {

    /**
     * F1：邮件资费查询
     *
     * @param request 资费查询请求（含会话头和业务参数）
     * @return 资费查询结果
     */
    PostageQueryResponse queryPostage(PostageQueryRequest request);

    /**
     * F2：邮件号码生成
     *
     * @param request 号码生成请求
     * @return 生成的邮件号码
     */
    MailNumberResponse generateMailNumber(MailNumberRequest request);

    /**
     * F3：业务办理-收寄订单提交
     *
     * @param body 收寄订单业务参数
     * @return 收寄结果（含交易流水号、资费明细）
     */
    OrderSubmitResponse submitOrder(OrderSubmitBody body);

    /**
     * F4：生成订单收款二维码
     *
     * @param body 二维码生成业务参数
     * @return 二维码链接、查询流水号等
     */
    QrCodeResponse generateQrCode(QrCodeBody body);

    /**
     * F5：支付状态查询
     *
     * @param body 支付状态查询业务参数
     * @return 支付状态（01成功/02失败/00支付中等）
     */
    PaymentStatusResponse queryPaymentStatus(PaymentStatusBody body);
}
