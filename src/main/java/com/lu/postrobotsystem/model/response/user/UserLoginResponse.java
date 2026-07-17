package com.lu.postrobotsystem.model.response.user;

import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录响应 DTO。
 * <p>
 * 用于封装用户登录成功后返回的响应数据，包含用户基本信息、
 * 访问令牌（accessToken）、刷新令牌（refreshToken）以及用户状态等。
 * 前端可将 token 存储于本地，用于后续请求的身份验证。
 * </p>
 *
 * @Author Dopamine
 * @Create 2026/6/16 23:43
 * @Version 1.0
 */
@Data
public class UserLoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 12L;

    @Schema(description ="id")
    private Long id;                            // 用户唯一标识 ID

    @Schema(description ="登录账号")
    private String username;                    // 用户的登录账号名称

    @Schema(description ="真实姓名")
    private String realName;                    // 用户的真实姓名

    @Schema(description = "手机号")
    private String phone;                       // 用户的手机号码

    @Schema(description = "邮箱")
    private String email;                       // 用户的电子邮箱地址

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;                  // 用户角色：ADMIN-管理员，OPERATOR-操作员，MAINTAINER-维护员

    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;                     // 用户账号状态：0-停用，1-启用

    @Schema(description = "访问令牌（accessToken）")
    private String token;                       // JWT 访问令牌，用于接口请求的身份验证

    @Schema(description = "刷新令牌（refreshToken）")
    private String refreshToken;                // 刷新令牌，用于在 accessToken 过期后获取新的令牌

    @Schema(description ="编辑时间")
    private LocalDateTime editTime;             // 用户信息的编辑时间

    @Schema(description = "创建时间")
    private LocalDateTime createTime;           // 用户账号的创建时间

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;           // 用户账号的最近更新时间


}
