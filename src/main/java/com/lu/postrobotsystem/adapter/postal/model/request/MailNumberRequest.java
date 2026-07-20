package com.lu.postrobotsystem.adapter.postal.model.request;

import com.lu.postrobotsystem.adapter.postal.model.SessionHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件号码生成完整请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailNumberRequest {

    /** 会话头 */
    private SessionHeader sessionHeader;

    /** 号码生成业务参数 */
    private MailNumberBody sessionBody;
}
