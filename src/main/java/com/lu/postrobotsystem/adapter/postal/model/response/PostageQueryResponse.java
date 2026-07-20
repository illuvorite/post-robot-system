package com.lu.postrobotsystem.adapter.postal.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 邮件资费查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostageQueryResponse {

    /** 是否成功 */
    private boolean success;

    /** 邮政侧错误码 */
    private String errorCode;

    /** 错误描述 */
    private String errorMessage;

    /** 总资费（分） */
    private BigDecimal totalPostage;

    /** 运费（分） */
    private BigDecimal freight;

    /** 保价费（分） */
    private BigDecimal insuranceFee;

    /** 包装费（分） */
    private BigDecimal packingFee;

    /** 其他费用（分） */
    private BigDecimal otherFee;

    /** 资费计算明细（JSON 文本） */
    private String detail;
}
