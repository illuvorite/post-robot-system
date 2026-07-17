package com.lu.postrobotsystem.model.request.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
@Schema(description = "用户登录")
public class UserLoginRequest implements Serializable{

        private static final long serialVersionUID = 1L;

        @Schema(description = "登录账号",requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @Schema(description = "密码",requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "邮箱")
        private String email;

}
