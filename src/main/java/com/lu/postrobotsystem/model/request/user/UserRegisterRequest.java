package com.lu.postrobotsystem.model.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号（可选，不传则默认用邮箱前缀）
     */
    @Schema(description = "用户账号")
    private String username;

    /**
     * 真实姓名（可选）
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 手机号（可选）
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 邮箱（可选，传了则绑定邮箱）
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 密码
     */
    @Schema(description = "用户密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 确认密码
     */
    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkPassword;

}
