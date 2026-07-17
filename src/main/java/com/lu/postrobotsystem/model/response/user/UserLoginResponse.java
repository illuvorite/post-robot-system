package com.lu.postrobotsystem.model.response.user;

import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录
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
    private Long id;

    @Schema(description ="登录账号")
    private String username;

    @Schema(description ="真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;

    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;

    @Schema(description ="编辑时间")
    private LocalDateTime editTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;


}
