package com.lu.postrobotsystem.model.request.user;

import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户新增请求
 */
@Data
@Schema(description = "用户新增")
public class UserAddRequest  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;


}
