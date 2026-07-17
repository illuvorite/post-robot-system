package com.lu.postrobotsystem.model.request.user;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户查询请求
 */
@Data
@Schema(description = "用户查询")
public class UserQueryRequest implements Serializable {


    private static final long serialVersionUID = 1L;


    @Schema(description = "用户ID（Snowflake）")
    private Long id;


    @Schema(description = "登录账号")
    private String userName;


    @Schema(description = "真实姓名")
    private String realName;


    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;

    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}

