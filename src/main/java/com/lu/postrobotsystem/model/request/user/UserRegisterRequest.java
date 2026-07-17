package com.lu.postrobotsystem.model.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求 DTO。
 * <p>
 * 用于接收新用户注册时提交的表单信息，包含账号、真实姓名、手机号、邮箱以及密码。
 * 账号不传时将默认使用邮箱前缀作为登录账号；密码和确认密码为必填项。
 * </p>
 */
@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户登录账号，可选。不传则默认用邮箱 `@` 前面的部分作为账号。
     */
    @Schema(description = "用户账号")
    private String username;

    /**
     * 用户真实姓名，可选。
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 用户手机号，可选。
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 用户邮箱地址，可选。传了则绑定邮箱，可用于找回密码或接收通知。
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 用户密码，必填。需满足密码复杂度要求。
     */
    @Schema(description = "用户密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 确认密码，必填。必须与 password 字段值一致。
     */
    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkPassword;

}
