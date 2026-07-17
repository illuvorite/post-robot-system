package com.lu.postrobotsystem.model.request.user;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.common.PageRequest;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户查询请求 DTO。
 * <p>
 * 用于接收管理端传递的用户查询条件，支持按用户 ID、登录账号、真实姓名、手机号、
 * 邮箱、角色、状态以及时间范围等多维度筛选。
 * 继承 {@link PageRequest}，支持分页查询。
 * </p>
 */
@Data
@Schema(description = "用户查询")
public class UserQueryRequest extends PageRequest implements Serializable {


    private static final long serialVersionUID = 1L;


    @Schema(description = "用户ID（Snowflake）")
    private Long id;                            // 用户ID（Snowflake 算法生成），精确匹配


    @Schema(description = "登录账号")
    private String userName;                    // 登录账号，精确匹配


    @Schema(description = "真实姓名")
    private String realName;                    // 用户的真实姓名，精确匹配


    @Schema(description = "手机号")
    private String phone;                       // 用户的手机号码，精确匹配

    @Schema(description = "邮箱")
    private String email;                       // 用户的电子邮箱地址，精确匹配

    @Schema(description = "角色（ADMIN/OPERATOR/MAINTAINER）")
    private UserRoleEnum role;                  // 用户角色枚举：ADMIN-管理员，OPERATOR-操作员，MAINTAINER-维护员

    @Schema(description = "状态（0-停用 1-启用）")
    private Integer status;                     // 用户账号状态：0-停用，1-启用

    @Schema(description = "创建时间")
    private LocalDateTime createTime;           // 用户账号的创建时间

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;           // 用户信息的最后更新时间

    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;                  // 逻辑删除标记：0-正常，1-已删除
}

