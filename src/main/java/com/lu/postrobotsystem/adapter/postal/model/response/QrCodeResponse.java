package com.lu.postrobotsystem.adapter.postal.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收款二维码生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {

    /** 是否成功 */
    private boolean success;

    /** 邮政侧错误码 */
    private String errorCode;

    /** 错误描述 */
    private String errorMessage;

    /** 平台流水号 */
    private String platformFlowNo;

    /** 支付流水号 */
    private String paymentFlowNo;

    /** 二维码链接 URL */
    private String qrCodeUrl;

    /** 支付查询流水号 */
    private String payQueryNo;
}
