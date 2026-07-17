package com.lu.postrobotsystem.controller;

import cn.hutool.core.util.StrUtil;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.request.user.UserLoginRequest;
import com.lu.postrobotsystem.model.request.user.UserRegisterRequest;
import com.lu.postrobotsystem.model.response.user.UserLoginResponse;
import com.lu.postrobotsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;
import static com.lu.postrobotsystem.exception.ResultCode.NOT_LOGIN_ERROR;

/**
 * 认证控制器。
 * <p>
 * 负责处理用户认证相关的 HTTP 请求，包括登录、注册、退出登录和令牌刷新。
 * 作为系统认证入口，所有认证操作均委托给 {@link UserService} 执行具体业务逻辑。
 * 该控制器不经过 Spring Security 的认证过滤器，是公开访问端点。
 * </p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理")
@RequiredArgsConstructor
public class AuthController {

    /** 用户服务层，处理认证相关的业务逻辑 */
    private final UserService userService;

    /**
     * 用户登录。
     * <p>
     * 支持通过用户名、邮箱或手机号三种方式进行登录。
     * 请求中包含的凭证信息被封装为 {@link UserLoginRequest}，交由 {@link UserService#userLogin} 完成
     * 身份校验，并返回包含 AccessToken 和 RefreshToken 的登录响应。
     * </p>
     *
     * @param request     登录请求体，包含 username/email/phone 和 password，已通过 {@code @Valid} 校验
     * @param httpRequest HTTP 请求对象，用于记录登录 IP 和 User-Agent 等信息
     * @return 统一响应结果，包含 {@link UserLoginResponse}（令牌、用户基本信息等），并附带"登录成功"提示
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录（支持账号/邮箱/手机号）")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request,
                                           HttpServletRequest httpRequest) {
        // 调用 userService 执行登录逻辑，传入多种可登录标识和密码及请求上下文
        UserLoginResponse response = userService.userLogin(
                request.getUsername(), request.getEmail(), request.getPhone(),
                request.getPassword(), httpRequest);
        return Result.success(response, "登录成功");
    }

    /**
     * 用户注册。
     * <p>
     * 接收用户名、密码和确认密码进行注册。
     * 参数校验由 {@code @Valid} 注解和 {@link UserService#userRegister} 内部共同完成。
     * 注册成功后返回新用户的 ID。
     * </p>
     *
     * @param request 注册请求体，包含 username、password 和 checkPassword
     * @return 统一响应结果，包含新注册用户的 ID，并附带"注册成功"提示
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        // 调用 userService 执行注册逻辑，返回新用户的 ID
        Long userId = userService.userRegister(
                request.getUsername(), request.getPassword(), request.getCheckPassword());
        return Result.success(userId, "注册成功");
    }

    /**
     * 退出登录。
     * <p>
     * 将当前请求携带的 Token 加入黑名单，使其立即失效。
     * 前端应在退出后清除本地存储的 Token。
     * </p>
     *
     * @param request HTTP 请求对象，用于从请求头中提取 Token
     * @return 统一响应结果，附带"退出成功"提示
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录（Token 加入黑名单）")
    public Result<Void> logout(HttpServletRequest request) {
        // 调用 userService 执行登出逻辑，将 Token 加入黑名单
        userService.userLogout(request);
        return Result.success(null, "退出成功");
    }

    /**
     * 刷新 Access Token。
     * <p>
     * 当 Access Token 过期时，使用有效的 Refresh Token 获取新的 Access Token。
     * 验证 Refresh Token 的有效性后，签发新的 Token 对。
     * 此接口对 Refresh Token 的合法性进行前置非空校验。
     * </p>
     *
     * @param refreshToken 刷新令牌字符串，不能为空
     * @return 统一响应结果，包含新的 {@link UserLoginResponse}（含新 Access Token 和 Refresh Token）
     * @throws RuntimeException 如果 refreshToken 为空，抛出 NOT_LOGIN_ERROR 异常
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Access Token")
    public Result<UserLoginResponse> refresh(@RequestParam String refreshToken) {
        // 前置校验：refreshToken 不能为空或空白
        ThrowUtils.throwIf(StrUtil.isBlank(refreshToken), NOT_LOGIN_ERROR, "refreshToken 不能为空");
        // 调用 userService 的令牌刷新方法，生成新的 Token 对
        UserLoginResponse response = userService.refreshToken(refreshToken);
        return Result.success(response, "Token 刷新成功");
    }
}
