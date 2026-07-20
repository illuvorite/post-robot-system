package com.lu.postrobotsystem.adapter.postal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YYRoot 通用报文包装器
 * <p>
 * 邮政接口统一报文结构。请求和响应均使用此包装：
 * <ul>
 *   <li>请求：SessionHeader（含签名） + SessionBody（业务数据）</li>
 *   <li>响应：SessionHeader（含平台返回信息） + SessionBody（业务结果）</li>
 * </ul>
 * </p>
 *
 * @param <T> SessionBody 的具体业务类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YYRoot<T> {

    /** 会话头 */
    private SessionHeader sessionHeader;

    /** 会话体（业务数据） */
    private T sessionBody;

    /**
     * 快速创建请求报文
     *
     * @param header 会话头
     * @param body   会话体
     * @param <T>    会话体类型
     * @return YYRoot 实例
     */
    public static <T> YYRoot<T> of(SessionHeader header, T body) {
        return new YYRoot<>(header, body);
    }
}
