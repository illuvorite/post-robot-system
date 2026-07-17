package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 对应数据库 user 表，存储系统用户的认证与基本信息，
 * 支持多角色（管理员/运营人员/维护人员）和账户启停管理。
 * 密码采用 BCrypt 加密存储，保障账户安全。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("user")
@Schema(description = "用户")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "用户ID（Snowflake）")
    private Long id;

    /** 登录账号，系统唯一 */
    @TableField("username")
    @Schema(description = "登录账号")
    private String username;

    /** 密码（BCrypt加密存储） */
    @TableField("password")
    @Schema(description = "密码（BCrypt加密）")
    private String password;

    /** 真实姓名 */
    @TableField("real_name")
    @Schema(description = "真实姓名")
    private String realName;

    /** 手机号 */
    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    /** 邮箱地址 */
    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    /** 用户角色（ADMIN-管理员 / OPERATOR-运营人员 / MAINTAINER-维护人员），参见 UserRoleEnum */
    @TableField("role")
    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;

    /** 用户状态（0-停用 / 1-启用） */
    @TableField("status")
    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 记录更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-正常 1-删除） */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
