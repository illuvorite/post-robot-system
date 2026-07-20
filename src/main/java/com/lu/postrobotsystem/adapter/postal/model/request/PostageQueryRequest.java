package com.lu.postrobotsystem.adapter.postal.model.request;

import com.lu.postrobotsystem.adapter.postal.model.SessionHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件资费查询完整请求
 * <p>
 * 包含 YYRoot 的会话头和资费查询业务体。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostageQueryRequest {

    /** 会话头 */
    private SessionHeader sessionHeader;

    /** 资费查询业务参数 */
    private PostageQueryBody sessionBody;
}
