package com.lu.postrobotsystem.common;


import lombok.Data;

import java.io.Serializable;


/**
 * 通用分页请求封装类
 * <p>
 * 为系统内所有分页查询操作提供统一的请求参数结构。
 * 使用时通过泛型 {@code <T>} 指定具体的查询条件类型，
 * 在 Controller 层接收前端分页参数，传递给 Service 层和 MyBatis-Plus 分页插件。
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>Controller 层接收前端分页参数，构造 PageRequest 对象</li>
 *   <li>Service 层将 PageRequest 转换为 MyBatis-Plus 的 {@code Page<T>} 对象</li>
 *   <li>{@link com.lu.postrobotsystem.config.MyBatisPlusConfig} 中的分页拦截器自动拼接 LIMIT/OFFSET 语句</li>
 * </ul>
 * </p>
 *
 * <p><b>字段说明：</b>
 * <ul>
 *   <li>{@link #pageNum} -- 当前页码，默认从第1页开始</li>
 *   <li>{@link #pageSize} -- 每页记录数，默认为10条</li>
 *   <li>{@link #sortField} -- 排序字段名，对应数据库列名</li>
 *   <li>{@link #sortOrder} -- 排序方向，默认为降序（descend）</li>
 * </ul>
 * </p>
 *
 * @param <T> 查询条件的实体类型
 * @author lu
 * @since 1.0.0
 */
@Data
public class PageRequest<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码，默认值为 1（从第一页开始） */
    private int pageNum = 1;

    /** 每页显示记录数，默认值为 10 */
    private int pageSize = 10;

    /** 排序字段名称，对应数据库表中的列名（如 "create_time"） */
    private String sortField;

    /** 排序方向，"ascend" 表示升序，"descend" 表示降序，默认为降序 */
    private String sortOrder = "descend";


}
