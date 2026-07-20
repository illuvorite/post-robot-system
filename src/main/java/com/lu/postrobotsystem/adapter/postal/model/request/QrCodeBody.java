package com.lu.postrobotsystem.adapter.postal.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收款二维码生成请求体（SessionBody）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeBody {

    /** 机构编号 */
    private String orgCode;

    /** 台席代码 */
    private String deskCode;

    /** 员工工号 */
    private String staffCode;

    /** 查询流水号（用于后续支付状态查询） */
    private String queryNo;

    /** 订单号 */
    private String orderNo;

    /** 支付流水号 */
    private String paymentFlowNo;

    /** 订单金额（分） */
    private String amount;
}
