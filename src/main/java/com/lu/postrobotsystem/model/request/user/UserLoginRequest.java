package com.lu.postrobotsystem.model.request.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求 DTO。
 * <p>
 * 用于接收用户登录时提交的凭证信息，支持账号密码登录以及手机号/邮箱辅助验证。
 * 登录账号和密码为必填项。
 * </p>
 */
@Data
@Schema(description = "用户登录")
public class UserLoginRequest implements Serializable{

        private static final long serialVersionUID = 1L;

        @Schema(description = "登录账号",requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;                // 登录账号，必填

        @Schema(description = "密码",requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;                // 登录密码，必填

        @Schema(description = "手机号")
        private String phone;                   // 手机号，可选，用于辅助验证

        @Schema(description = "邮箱")
        private String email;                   // 邮箱地址，可选，用于辅助验证

}
