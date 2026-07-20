package com.lu.postrobotsystem.common;

import com.lu.postrobotsystem.adapter.postal.model.response.PaymentStatusResponse;
import com.lu.postrobotsystem.adapter.postal.model.response.QrCodeResponse;
import com.lu.postrobotsystem.adapter.postal.service.PostalAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮政支付接口客户端（向后兼容门面）
 * <p>
 * 保留此类作为业务层调用的入口点，内部实现已迁移至
 * {@link PostalAdapterService} + {@link com.lu.postrobotsystem.adapter.postal.spi.PostalApi} 适配层架构。
 * 现有 {@link com.lu.postrobotsystem.service.impl.OrderServiceImpl} 和
 * {@link com.lu.postrobotsystem.service.impl.PaymentServiceImpl} 无需修改即可继续使用此类。
 * </p>
 *
 * <p><b>Mock 模式：</b>当 {@code postal.mock.enabled=true} 时（默认），所有接口返回模拟数据。
 * 生产环境设置 {@code postal.mock.enabled=false} 即可切换到真实 HTTP 调用。</p>
 *
 * <p><b>支付信息安全：</b>
 * - 二维码原始响应数据不持久化到数据库（仅存链接 URL）<br>
 * - 日志输出自动脱敏，遮盖签名等敏感字段<br>
 * - 流水记录仅存非敏感字段摘要</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostalApiClient {

    private final PostalAdapterService postalAdapterService;

    /**
     * 生成邮政收款二维码
     *
     * @param orderNo       订单号
     * @param totalAmount   订单总金额（单位：分）
     * @param paymentFlowNo 支付流水号
     * @return 二维码响应
     */
    public PostalQrResponse generateQrCode(String orderNo, String totalAmount, String paymentFlowNo) {
        QrCodeResponse response = postalAdapterService.generateQrCode(orderNo, totalAmount, paymentFlowNo);

        // 转换为旧版响应格式（保持向后兼容）
        PostalQrResponse resp = new PostalQrResponse();
        resp.setSuccess(response.isSuccess());
        resp.setQrCodeUrl(response.getQrCodeUrl());
        resp.setTransactionId(response.getPlatformFlowNo());
        resp.setPayQueryNo(response.getPayQueryNo());
        return resp;
    }

    /**
     * 查询支付结果
     *
     * @param payQueryNo  支付查询流水号
     * @param orderNo     订单号
     * @return 支付查询结果
     */
    public PaymentQueryResult queryPaymentStatus(String payQueryNo, String orderNo) {
        PaymentStatusResponse response = postalAdapterService.queryPaymentStatus(payQueryNo, "");

        // 转换为旧版响应格式
        PaymentQueryResult result = new PaymentQueryResult();
        result.setSuccess(response.isSuccess());
        result.setPaid("01".equals(response.getPayStatus()));
        result.setPlatformFlowNo(response.getPlatformFlowNo());
        // 支付状态映射：01→支付成功, 00→支付中, 02→支付失败
        String statusDesc = switch (response.getPayStatus()) {
            case "01" -> "支付成功";
            case "02" -> "支付失败";
            case "00" -> "支付中";
            default -> "未知状态";
        };
        result.setStatusDesc(statusDesc);
        return result;
    }

    /**
     * 二维码响应（向后兼容）
     */
    public static class PostalQrResponse {
        private boolean success;
        private String qrCodeUrl;
        private String transactionId;
        private String payQueryNo;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getPayQueryNo() { return payQueryNo; }
        public void setPayQueryNo(String payQueryNo) { this.payQueryNo = payQueryNo; }
    }

    /**
     * 支付查询结果（向后兼容）
     */
    public static class PaymentQueryResult {
        private boolean success;
        private boolean paid;
        private String platformFlowNo;
        private String statusDesc;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
        public String getPlatformFlowNo() { return platformFlowNo; }
        public void setPlatformFlowNo(String platformFlowNo) { this.platformFlowNo = platformFlowNo; }
        public String getStatusDesc() { return statusDesc; }
        public void setStatusDesc(String statusDesc) { this.statusDesc = statusDesc; }
    }
}
