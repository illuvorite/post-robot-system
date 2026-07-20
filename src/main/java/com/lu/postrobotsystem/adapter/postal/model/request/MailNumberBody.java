package com.lu.postrobotsystem.adapter.postal.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件号码生成请求体（SessionBody）
 * <p>
 * 入参含省份代码、机构编号、业务产品代码/名称、数据来源代码等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailNumberBody {

    /** 省份代码 */
    private String provinceCode;

    /** 机构编号 */
    private String orgCode;

    /** 业务产品代码 */
    private String productCode;

    /** 业务产品名称 */
    private String productName;

    /** 数据来源代码 */
    private String sourceCode;
}
