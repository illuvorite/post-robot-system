package com.lu.postrobotsystem.adapter.postal.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付状态查询请求体（SessionBody）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusBody {

    /** 查询流水号 */
    private String queryNo;

    /** 机构编号 */
    private String orgCode;

    /** 支付流水号 */
    private String paymentFlowNo;
}
