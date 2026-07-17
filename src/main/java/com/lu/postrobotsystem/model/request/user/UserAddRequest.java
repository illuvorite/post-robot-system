package com.lu.postrobotsystem.model.request.user;

import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户新增请求 DTO。
 * <p>
 * 用于接收管理端新增用户时提交的信息，包含登录账号、真实姓名、手机号、邮箱以及角色分配。
 * 新增用户后系统会自动为其初始化密码（通常为默认密码或通过其他方式下发）。
 * </p>
 */
@Data
@Schema(description = "用户新增")
public class UserAddRequest  implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @NotNull(message = "角色不能为空")
    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;
}
