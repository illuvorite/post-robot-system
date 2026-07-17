package com.lu.postrobotsystem.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求封装类
 * <p>
 * 为系统内所有删除操作提供统一的请求参数结构。
 * 前端在发起删除请求时，仅需传递待删除记录的ID，
 * 后端通过此对象接收参数并进行逻辑删除或物理删除。
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>Controller 层接收前端 JSON 请求体，反序列化为 DeleteRequest 对象</li>
 *   <li>Service 层根据 {@link #id} 执行删除逻辑</li>
 * </ul>
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * 待删除记录的唯一标识ID
     * <p>对应数据库表的主键字段，用于定位需要删除的记录。</p>
     */
    private Long id;

    /** 序列化版本号，用于确保反序列化时的版本兼容性 */
    private static final long serialVersionUID = 1L;
}
