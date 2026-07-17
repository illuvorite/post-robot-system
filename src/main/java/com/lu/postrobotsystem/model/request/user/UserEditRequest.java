package com.lu.postrobotsystem.model.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑请求
 */
@Data
@Schema(description = "用户编辑信息")
public class UserEditRequest implements Serializable {

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;


    @Schema(description = "手机号")
    private String phone;


    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "当前密码（修改密码时必填）")
    private String password;

    @Schema(description = "新密码（修改密码时必填）")
    private String newPassword;

    private static final long serialVersionUID = 1L;
}
