package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付流水记录实体类
 * <p>
 * 对应数据库 payment_flow_record 表，记录订单支付过程中每一次与支付平台的交互记录，
 * 包括二维码请求、支付回调、支付状态查询等。
 * </p>
 *
 * <p><b>支付信息安全说明：</b><br>
 * 本表<b>不存储</b>任何敏感信息，包括：<br>
 * - 支付凭证原始数据（如完整响应报文、二维码 Base64 内容）<br>
 * - 卡号、CVV、有效期等金融敏感字段<br>
 * - 签名原文或加密密钥<br>
 * 仅记录非敏感的业务标识字段摘要，用于全链路追溯和问题排查。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("payment_flow_record")
@Schema(description = "支付流水记录")
public class PaymentFlowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "记录ID（Snowflake）")
    private Long id;

    /** 关联的订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 订单号（冗余字段，便于独立查询免关联） */
    @TableField("order_no")
    @Schema(description = "订单号")
    private String orderNo;

    /** 支付流水号 */
    @TableField("payment_flow_no")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 流水类型（QR_REQUEST-二维码请求 CALLBACK-支付回调 QUERY-支付查询） */
    @TableField("flow_type")
    @Schema(description = "流水类型（QR_REQUEST/CALLBACK/QUERY）")
    private String flowType;

    /** 请求数据摘要（仅存非敏感字段的JSON摘要，不含卡号/签名原文等敏感信息） */
    @TableField("req_data_digest")
    @Schema(description = "请求数据摘要（非敏感字段）")
    private String reqDataDigest;

    /** 响应数据摘要（仅存非敏感字段的JSON摘要，不含卡号/签名原文等敏感信息） */
    @TableField("resp_data_digest")
    @Schema(description = "响应数据摘要（非敏感字段）")
    private String respDataDigest;

    /** 请求来源IP */
    @TableField("source_ip")
    @Schema(description = "请求来源IP")
    private String sourceIp;

    /** 追踪ID */
    @TableField("trace_id")
    @Schema(description = "追踪ID")
    private String traceId;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
