package com.lu.postrobotsystem.model.response.user;

import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息响应 DTO。
 * <p>
 * 用于封装用户相关的响应数据，包含用户 ID、登录账号、真实姓名、手机号、
 * 邮箱、角色、状态以及创建时间等。作为服务层返回给控制层/前端的数据载体。
 * 实现了 {@link Serializable} 接口，支持序列化操作。
 * </p>
 */
@Data
public class UserResponse implements Serializable {

    @Schema(description = "用户ID")
    private Long id;                            // 用户唯一标识符（Snowflake 算法生成）

    @Schema(description = "登录账号")
    private String username;                    // 用户的登录账号名称

    @Schema(description = "真实姓名")
    private String realName;                    // 用户的真实姓名

    @Schema(description = "手机号")
    private String phone;                       // 用户的手机号码

    @Schema(description = "邮箱")
    private String email;                       // 用户的电子邮箱地址

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;                  // 用户角色枚举：ADMIN-管理员，OPERATOR-操作员，MAINTAINER-维护员

    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;                     // 用户账号状态：0-停用，1-启用

    @Schema(description = "创建时间")
    private LocalDateTime createTime;           // 用户账号的创建时间

    // 序列化版本号，用于版本控制
    private static final long serialVersionUID = 1L;
}
