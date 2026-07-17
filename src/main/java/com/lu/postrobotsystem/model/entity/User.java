package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("user")
@Schema(description = "用户")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "用户ID（Snowflake）")
    private Long id;

    @TableField("username")
    @Schema(description = "登录账号")
    private String username;

    @TableField("password")
    @Schema(description = "密码（BCrypt加密）")
    private String password;

    @TableField("real_name")
    @Schema(description = "真实姓名")
    private String realName;

    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    @TableField("role")
    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;

    @TableField("status")
    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
