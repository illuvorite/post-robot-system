package com.lu.postrobotsystem.adapter.postal.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付状态查询响应
 * <p>
 * 支付状态编码（邮政侧）：01=成功 02=失败 03=已退款 05=部分退款 00=支付中
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    /** 是否成功 */
    private boolean success;

    /** 邮政侧错误码 */
    private String errorCode;

    /** 错误描述 */
    private String errorMessage;

    /** 支付状态编码（01=成功 02=失败 03=已退款 05=部分退款 00=支付中） */
    private String payStatus;

    /** 平台流水号 */
    private String platformFlowNo;

    /** 支付完成时间（yyyyMMddHHmmss） */
    private String paidTime;

    /** 支付状态中文描述 */
    private String statusDesc;
}
