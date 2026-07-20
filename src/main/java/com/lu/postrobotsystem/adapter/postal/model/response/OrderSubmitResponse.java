package com.lu.postrobotsystem.adapter.postal.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收寄订单提交响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitResponse {

    /** 是否成功 */
    private boolean success;

    /** 邮政侧错误码 */
    private String errorCode;

    /** 错误描述 */
    private String errorMessage;

    /** 交易流水号 */
    private String transactionId;

    /** 邮件号码 */
    private String mailNo;

    /** 总资费（分） */
    private BigDecimal totalPostage;

    /** 应收总资费（分） */
    private BigDecimal receivablePostage;

    /** 资费明细列表 */
    private List<PostageDetailItem> postageDetails;

    /** 资费明细项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostageDetailItem {
        /** 资费项目代码 */
        private String itemCode;
        /** 资费项目名称 */
        private String itemName;
        /** 资费金额（分） */
        private BigDecimal amount;
    }
}
