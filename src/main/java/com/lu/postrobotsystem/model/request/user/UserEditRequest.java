package com.lu.postrobotsystem.model.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑请求 DTO。
 * <p>
 * 用于接收用户信息编辑请求。id 为空时默认修改当前登录用户的信息；
 * 修改密码时需要同时传入当前密码和新密码。
 * </p>
 */
@Data
@Schema(description = "用户编辑信息")
public class UserEditRequest implements Serializable {

    @Schema(description = "用户ID（编辑他人时传入，默认当前用户）")
    private Long id;                            // 用户ID，编辑他人信息时传入，不传则默认修改当前登录用户

    @Schema(description = "登录账号")
    private String username;                    // 登录账号

    @Schema(description = "真实姓名")
    private String realName;                    // 用户的真实姓名


    @Schema(description = "手机号")
    private String phone;                       // 用户的手机号码


    @Schema(description = "邮箱")
    private String email;                       // 用户的电子邮箱地址

    @Schema(description = "当前密码（修改密码时必填）")
    private String password;                    // 当前密码，修改密码时为必填项，用于身份验证

    @Schema(description = "新密码（修改密码时必填）")
    private String newPassword;                 // 新密码，修改密码时为必填项

    private static final long serialVersionUID = 1L;
}
