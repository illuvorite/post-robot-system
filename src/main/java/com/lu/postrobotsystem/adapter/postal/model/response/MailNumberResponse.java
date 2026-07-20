package com.lu.postrobotsystem.adapter.postal.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件号码生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailNumberResponse {

    /** 是否成功 */
    private boolean success;

    /** 邮政侧错误码 */
    private String errorCode;

    /** 错误描述 */
    private String errorMessage;

    /** 生成的邮件号码 */
    private String mailNo;
}
