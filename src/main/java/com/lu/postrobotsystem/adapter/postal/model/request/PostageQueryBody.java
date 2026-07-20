package com.lu.postrobotsystem.adapter.postal.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 邮件资费查询请求体（SessionBody）
 * <p>
 * 入参含产品代码、收寄省/市/区县、寄达省/市/区县、是否保价、保价金额、长宽高、重量等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostageQueryBody {

    /** 产品代码（如 YT-标准快递） */
    private String productCode;

    /** 收寄省份代码 */
    private String sendProvince;

    /** 收寄城市代码 */
    private String sendCity;

    /** 收寄区县代码 */
    private String sendDistrict;

    /** 寄达省份代码 */
    private String destProvince;

    /** 寄达城市代码 */
    private String destCity;

    /** 寄达区县代码 */
    private String destDistrict;

    /** 是否保价（true=保价，false=不保价） */
    private Boolean insured;

    /** 保价金额（分） */
    private Long insuredAmount;

    /** 邮件长度（mm） */
    private Integer length;

    /** 邮件宽度（mm） */
    private Integer width;

    /** 邮件高度（mm） */
    private Integer height;

    /** 重量（克） */
    private Integer weight;
}
